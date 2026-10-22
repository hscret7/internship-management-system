#!/usr/bin/env python3
"""No Docker or real backup path: every file is inside TemporaryDirectory."""
import contextlib
import fcntl
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import time
import unittest

spec = importlib.util.spec_from_file_location('job', Path(__file__).with_name('run-be1-backup.py'))
job = importlib.util.module_from_spec(spec)
spec.loader.exec_module(job)


class BackupJobTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name) / 'output'
        self.root.mkdir()
        self.helper = Path(self.tmp.name) / 'fixture-helper.sh'
        self.helper.write_text('''set -e
p=$(mktemp -d "$4/be1-$2-fixture.XXXXXX")
printf 'fixture SQL\\n' > "$p/database.sql"
(cd "$p" && sha256sum database.sql > SHA256SUMS)
printf 'database=%s\\n' "$2" > "$p/metadata.txt"
''')
        self.cfg = dict(enabled=True, container='fixture', database='fixture', client_cnf='/tmp/test.cnf',
                        output_dir=str(self.root), timeout_seconds=5, retention_enabled=True,
                        keep_days=7, min_copies=2)

    def run_job(self, prune=False):
        with contextlib.redirect_stdout(io.StringIO()) as logs:
            result = job.run(self.cfg, self.helper, prune)
        return result, logs.getvalue()

    def bundle(self, name, age, valid=True, database='fixture', marked=True):
        p = self.root / ('be1-' + database + '-' + name)
        p.mkdir()
        payload = b'fixture SQL\n'
        (p / 'database.sql').write_bytes(payload)
        (p / 'SHA256SUMS').write_text(hashlib.sha256(payload).hexdigest() + '  database.sql\n')
        (p / 'metadata.txt').write_text('database=' + database + '\n')
        if marked:
            job.write_marker(p, database, time.time() - age * 86400)
        if not valid:
            (p / 'database.sql').write_text('corrupted')
        return p

    def test_disabled_job_does_not_create_directory_or_start_helper(self):
        self.cfg = dict(enabled=False)
        self.helper.unlink()
        self.assertEqual(self.run_job()[0], 0)
        self.assertEqual(list(self.root.iterdir()), [])

    def test_success_marks_only_verified_new_bundle(self):
        status, logs = self.run_job()
        self.assertEqual(status, 0)
        self.assertIn('succeeded', logs)
        bundles = job.retention_candidates(self.root, 'fixture')
        self.assertEqual(len(bundles), 1)
        self.assertTrue(job.verify_bundle(bundles[0][1], 'fixture'))

    def test_lock_prevents_second_job_without_backup_or_prune(self):
        old = self.bundle('old', 30)
        with (self.root / '.be1-backup.lock').open('w') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            status, logs = self.run_job(True)
        self.assertEqual(status, 75)
        self.assertIn('busy', logs)
        self.assertTrue(old.exists())
        self.assertEqual(len(job.retention_candidates(self.root, 'fixture')), 1)

    def test_failed_dump_never_prunes_existing_copies(self):
        old = self.bundle('old', 30)
        self.helper.write_text('exit 9\n')
        self.assertEqual(self.run_job(True)[0], 1)
        self.assertTrue(old.exists())

    def test_retention_keeps_minimum_and_ignores_foreign_unmarked_corrupt_and_symlink(self):
        oldest = self.bundle('oldest', 40)
        protected = self.bundle('protected', 30)
        foreign = self.bundle('foreign', 40, database='another')
        unmarked = self.bundle('unmarked', 40, marked=False)
        corrupt = self.bundle('corrupt', 40, valid=False)
        link = self.root / 'be1-fixture-link'
        link.symlink_to(foreign, target_is_directory=True)
        self.assertEqual(self.run_job(True)[0], 0)
        self.assertFalse(oldest.exists())
        for p in (protected, foreign, unmarked, corrupt, link):
            self.assertTrue(p.exists(), str(p))
        self.assertEqual(len(job.retention_candidates(self.root, 'fixture')), 2)

    def test_retention_requires_both_config_and_runtime_opt_in(self):
        old = self.bundle('old', 40)
        self.cfg['min_copies'] = 1
        self.assertEqual(self.run_job(False)[0], 0)
        self.assertTrue(old.exists())
        self.cfg['retention_enabled'] = False
        self.assertEqual(self.run_job(True)[0], 0)
        self.assertTrue(old.exists())

    def test_tampered_new_bundle_does_not_trigger_retention(self):
        old = self.bundle('old', 40)
        self.helper.write_text(self.helper.read_text() + 'printf bad >> "$p/database.sql"\n')
        self.assertEqual(self.run_job(True)[0], 1)
        self.assertTrue(old.exists())

    def test_output_and_lock_symlinks_are_rejected(self):
        link = Path(self.tmp.name) / 'linked'
        link.symlink_to(self.root, target_is_directory=True)
        self.cfg['output_dir'] = str(link)
        with self.assertRaises(ValueError):
            self.run_job()
        self.cfg['output_dir'] = str(self.root)
        (self.root / '.be1-backup.lock').symlink_to(Path(self.tmp.name) / 'other')
        with self.assertRaises(OSError):
            self.run_job()

    def test_timeout_blocks_followup_jobs_until_operator_review(self):
        self.cfg['timeout_seconds'] = 1
        self.helper.write_text('sleep 10\n')
        self.assertEqual(self.run_job(True)[0], 1)
        self.assertTrue((self.root / '.be1-backup.blocked').exists())
        self.helper.write_text('exit 0\n')
        status, logs = self.run_job(True)
        self.assertEqual(status, 1)
        self.assertIn('previous_timeout_requires_review', logs)

    def test_invalid_policy_is_rejected_before_execution(self):
        p = Path(self.tmp.name) / 'config.json'
        for key, value in [('min_copies', 0), ('keep_days', -1), ('enabled', 'true'), ('timeout_seconds', True)]:
            cfg = dict(self.cfg)
            cfg[key] = value
            p.write_text(json.dumps(cfg))
            with self.assertRaises(ValueError):
                job.load_config(p)


if __name__ == '__main__':
    unittest.main()
