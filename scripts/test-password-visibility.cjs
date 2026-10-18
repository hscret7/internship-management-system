/* Run with Node.js: node scripts/test-password-visibility.cjs
 * Exercises the real shared script with a minimal DOM, without dependencies.
 */
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

const secret = 'Private password: Đặng 123!';
const documentListeners = {};
const windowListeners = {};
const buttons = [];
const formListeners = {};
const inputs = ['password', 'confirmPassword'].map(id => ({
    id, type: 'password', value: secret, dataset: {},
    form: {addEventListener(event, fn) {
        (formListeners[event] ||= []).push(fn);
    }},
    insertAdjacentElement(position, element) {
        assert.equal(position, 'afterend');
        this.button = element;
        buttons.push(element);
    }
}));
const document = {
    querySelector: () => null,
    querySelectorAll(selector) {
        if (selector === 'input[type="password"]') return inputs.filter(input => input.type === 'password');
        return [];
    },
    addEventListener(event, fn) { (documentListeners[event] ||= []).push(fn); },
    createElement(tag) {
        assert.equal(tag, 'button');
        return {
            attrs: {},
            setAttribute(key, value) { this.attrs[key] = value; },
            addEventListener(event, fn) { this[event] = fn; }
        };
    }
};
const window = {
    addEventListener(event, fn) { (windowListeners[event] ||= []).push(fn); }
};
const script = fs.readFileSync(path.join(__dirname, '../src/main/webapp/assets/js/app.js'), 'utf8');
vm.runInNewContext(script, {document, window});
const fire = (registry, event) => (registry[event] || []).forEach(fn => fn());
fire(documentListeners, 'DOMContentLoaded');
assert.equal(buttons.length, 2);
for (const input of inputs) {
    assert.equal(input.button.type, 'button');
    assert.equal(input.button.attrs['aria-controls'], input.id);
    assert.equal(input.button.attrs['aria-pressed'], 'false');
    assert.equal(input.type, 'password');
}
inputs[0].button.click();
assert.equal(inputs[0].type, 'text');
assert.equal(inputs[0].button.attrs['aria-pressed'], 'true');
assert.equal(inputs[1].type, 'password', 'each toggle affects only its field');
assert.equal(inputs[0].value, secret);
inputs[0].button.click();
assert.equal(inputs[0].type, 'password');
inputs[0].button.click();
fire(formListeners, 'submit');
assert.equal(inputs[0].type, 'password');
inputs.forEach(input => input.button.click());
fire(windowListeners, 'pageshow');
assert.ok(inputs.every(input => input.type === 'password'));
fire(windowListeners, 'pageshow');
assert.equal(buttons.length, 2, 'history restoration must not add controls');
// A second initialization must also preserve one control per field.
fire(documentListeners, 'DOMContentLoaded');
assert.equal(buttons.length, 2);
for (const input of inputs) {
    assert.equal(input.value, secret);
    assert.ok(!JSON.stringify(input.button).includes(secret), 'never copy password into button HTML or attributes');
}
console.log('PASS: password toggles, independent fields, ARIA, unchanged values, no duplicate controls, submit/history reset, no secret copied.');
