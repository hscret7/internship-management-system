#!/usr/bin/env python3
"""Opt-in backup job with a process lock, structured status and bounded retention."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import shutil
import stat
import subprocess
import sys
import time

MARKER = 'be1-backup-job-v1'


def emit(event, **fields):
    print(json.dumps(dict(event=event, utc=time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), **fields)), flush=True)


def load_config(path):
    path = Path(path)
    if not path.is_file() or path.is_symlink():
        raise ValueError('Configuration must be a regular file')
    cfg = json.loads(path.read_text())
    if not isinstance(cfg, dict) or type(cfg.get('enabled')) is not bool:
        raise ValueError('enabled must be true or false')
    if not cfg['enabled']:
        return cfg
    for key in ('container', 'database', 'client_cnf', 'output_dir'):
        if not isinstance(cfg.get(key), str) or not cfg[key]:
            raise ValueError('Missing required configuration field: ' + key)
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*', cfg['database']):
        raise ValueError('Invalid database name')
    if not cfg['client_cnf'].startswith('/') or not Path(cfg['output_dir']).is_absolute():
        raise ValueError('Client option file and output directory must be absolute')
    cfg.setdefault('retention_enabled', False)
    if type(cfg['retention_enabled']) is not bool:
        raise ValueError('retention_enabled must be true or false')
    if cfg['retention_enabled']:
        for key in ('keep_days', 'min_copies'):
            if type(cfg.get(key)) is not int or cfg[key] < 1:
                raise ValueError(key + ' must be a positive integer')
    cfg.setdefault('timeout_seconds', 1800)
    if type(cfg['timeout_seconds']) is not int or cfg['timeout_seconds'] < 1:
        raise ValueError('timeout_seconds must be positive')
    return cfg


def file_is_regular(path):
    try:
        return stat.S_ISREG(path.lstat().st_mode)
    except FileNotFoundError:
        return False


def verify_bundle(path, database):
    """Refuse symlinks, incomplete bundles and another database's metadata."""
    if path.is_symlink() or not path.is_dir():
        return False
    if not all(file_is_regular(path / name) for name in ('database.sql', 'SHA256SUMS', 'metadata.txt')):
        return False
    try:
        metadata = dict(line.split('=', 1) for line in (path / 'metadata.txt').read_text().splitlines())
        if metadata.get('database') != database:
            return False
        expected = (path / 'SHA256SUMS').read_text().strip()
        if not re.fullmatch(r'[0-9a-f]{64}  database\.sql', expected):
            return False
        sha = hashlib.sha256()
        with (path / 'database.sql').open('rb') as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                sha.update(chunk)
        return sha.hexdigest() == expected.split()[0] and (path / 'database.sql').stat().st_size > 0
    except (OSError, ValueError):
        return False


def write_marker(bundle, database, now):
    marker = bundle / '.be1-job.json'
    # Do not follow any existing marker; only newly published bundles are marked.
    with marker.open('x') as stream:
        json.dump(dict(format=MARKER, database=database, created_epoch=now), stream)


def retention_candidates(root, database):
    result = []
    for path in root.iterdir():
        if not path.name.startswith('be1-' + database + '-') or path.is_symlink():
            continue
        marker = path / '.be1-job.json'
        if not file_is_regular(marker):
            continue
        try:
            item = json.loads(marker.read_text())
            created = item.get('created_epoch')
            if item.get('format') != MARKER or item.get('database') != database:
                continue
            if type(created) not in (float, int) or not (0 <= created <= time.time() + 60):
                continue
            if verify_bundle(path, database):
                result.append((float(created), path))
        except (OSError, ValueError, AttributeError):
            continue
    return sorted(result, key=lambda pair: (pair[0], pair[1].name), reverse=True)


def prune(root, cfg, now):
    # Keep at least the newest min_copies of VERIFIED, explicitly marked bundles.
    candidates = retention_candidates(root, cfg['database'])
    cutoff = now - cfg['keep_days'] * 86400
    for created, path in candidates[cfg['min_copies']:]:
        if created < cutoff:
            shutil.rmtree(path)
            emit('pruned', bundle=str(path))


def run(cfg, helper, allow_prune=False):
    if not cfg['enabled']:
        emit('disabled')
        return 0
    root = Path(cfg['output_dir'])
    if root.is_symlink():
        raise ValueError('Output directory must not be a symlink')
    root.mkdir(parents=True, exist_ok=True, mode=0o700)
    root = root.resolve()
    # File lock survives crashes without stale PID cleanup. Refuse lock symlinks.
    fd = os.open(root / '.be1-backup.lock', os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, 'w') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            emit('busy')
            return 75
        if (root / '.be1-backup.blocked').exists():
            emit('failed', reason='previous_timeout_requires_review')
            return 1
        before = set(root.iterdir())
        emit('started', database=cfg['database'])
        started = time.monotonic()
        try:
            # No shell interpolation; no password or configuration contents logged.
            with subprocess.Popen(['bash', str(helper), cfg['container'], cfg['database'],
                                   cfg['client_cnf'], str(root)], stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE, text=True, start_new_session=True) as process:
                try:
                    process.communicate(timeout=cfg['timeout_seconds'])
                except subprocess.TimeoutExpired:
                    # Kill the local process group, then block subsequent jobs until reviewed.
                    os.killpg(process.pid, signal.SIGKILL)
                    process.communicate()
                    (root / '.be1-backup.blocked').write_text('Review server-side mysqldump before removing this marker.\n')
                    emit('failed', reason='timeout_requires_review')
                    return 1
                if process.returncode != 0:
                    emit('failed', reason='dump_failed', exit_code=process.returncode)
                    return 1
        except OSError:
            emit('failed', reason='helper_start_failed')
            return 1
        new = [p for p in root.iterdir() if p not in before
               and p.name.startswith('be1-' + cfg['database'] + '-')]
        if len(new) != 1 or not verify_bundle(new[0], cfg['database']):
            emit('failed', reason='bundle_verification_failed')
            return 1
        bundle = new[0]
        write_marker(bundle, cfg['database'], time.time())
        if allow_prune and cfg['retention_enabled']:
            prune(root, cfg, time.time())
        emit('succeeded', bundle=str(bundle), seconds=round(time.monotonic() - started, 3))
        return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', required=True)
    parser.add_argument('--prune', action='store_true', help='Also apply the explicitly enabled retention policy after a verified backup')
    args = parser.parse_args()
    os.umask(0o077)
    try:
        return run(load_config(args.config), Path(__file__).with_name('backup-be1-mysql.sh'), args.prune)
    except (OSError, ValueError, json.JSONDecodeError):
        emit('failed', reason='configuration_or_filesystem_error')
        return 1


if __name__ == '__main__':
    sys.exit(main())
