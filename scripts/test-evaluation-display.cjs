const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('src/main/webapp/assets/js/fe3.js', 'utf8');
// Execute the actual rendering functions with a minimal text-node DOM, without network or a browser.
const start = source.indexOf('    function assessmentCard(');
const end = source.indexOf('    async function evaluationPage(', start);
function node(tag, text, className) { return { tag, text, className, children: [],
    appendChild(child) { this.children.push(child); }, replaceChildren(...children) { this.children = children; } }; }
const sandbox = { node, moment: value => value };
vm.runInNewContext(source.slice(start, end), sandbox);
function text(tree) { return [tree.text, ...tree.children.map(text)].filter(Boolean).join(' '); }
const card = sandbox.assessmentCard({ mentorName: 'Mentor A', createdAt: '2026-10-18', skillsScore: 2, attitudeScore: 5,
    comment: '<img src=x onerror=alert(1)>\nNhận xét nguyên văn.' }, 0);
assert.match(text(card), /Kỹ năng 2 \/ 5/);
assert.match(text(card), /Thái độ 5 \/ 5/);
assert.match(text(card), /Mentor A/);
assert.match(text(card), /2026-10-18/);
assert.match(text(card), /Mới nhất/);
assert.match(text(card), /<img src=x onerror=alert\(1\)>/);
assert.doesNotMatch(text(card), /Xuất sắc|Giỏi|Khá|\/ 10|\/100/);
const old = node('div'); sandbox.showEvaluation({ score: 0, createdAt: '2026-01-01', comment: 'Legacy zero' }, old);
assert.match(text(old), /0\/100/); assert.match(text(old), /Legacy zero/);
assert.doesNotMatch(text(old), /Chưa đạt|\/5/);
console.log('PASS: separate criteria, actual mentor/time, literal comment, no invented grade/conversion and legacy zero preserved');
