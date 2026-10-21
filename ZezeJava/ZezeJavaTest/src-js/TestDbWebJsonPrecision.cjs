'use strict';

// Run with node --test. The production HTML supplies all parsing, display and write logic.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');
const htmlPath = path.resolve(__dirname, '../../ZezeJava/src/main/resources/DbWebIndex.html');
const script = fs.readFileSync(htmlPath, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1];

async function page(raw) {
    const elements = {
        tableList: {value: 'table'}, keyList: {value: '17', selectedIndex: 0},
        jsonEditor: {value: '', disabled: true}, nextPage: {disabled: true}
    };
    const writes = [], alerts = [];
    const context = vm.createContext({
        URLSearchParams, location: {search: ''},
        localStorage: {getItem: () => 'test-token', setItem() {}},
        document: {getElementById: id => elements[id]},
        alert: value => alerts.push(value), confirm: () => true, prompt: () => null,
        fetch: async (url, options) => {
            if (url.includes('/PutRecord')) writes.push({url: new URL(url, 'http://localhost'), body: options.body});
            return {ok: true, status: 200,
                json: async () => url.includes('/ListTable') ? [] : JSON.parse(raw),
                text: async () => raw};
        }
    });
    vm.runInContext(script, context, {filename: htmlPath});
    await vm.runInContext('loadValue()', context);
    return {elements, writes, alerts, call: expression => vm.runInContext(expression, context)};
}

for (const literal of ['9007199254740993', '-9007199254740993', '9223372036854775807',
    '-9223372036854775808', '123456789.123456789123456789', '-0']) {
    test('load and untouched save preserve the exact JSON numeric token: ' + literal, async () => {
        const raw = '{\n  "long2": ' + literal + '\n}';
        const p = await page(raw);
        await p.call('saveValue()');
        assert.equal(p.writes.length, 1);
        assert.equal(p.writes[0].url.searchParams.get('t'), 'table');
        assert.equal(p.writes[0].url.searchParams.get('k'), '17');
        assert.equal(p.writes[0].body, raw,
            'the actual PUT must preserve the raw number, not its rounded JavaScript Number');
    });
}

test('safe integer values still load and save', async () => {
    const raw = JSON.stringify({number: Number.MAX_SAFE_INTEGER}, null, 2);
    const p = await page(raw);
    await p.call('saveValue()');
    assert.equal(p.writes[0].body, raw);
});

test('strings, booleans, nulls, arrays and nested objects retain their JSON representation', async () => {
    const raw = JSON.stringify({text: '中文 " quoted \\ \n', flag: true, absent: null,
        list: [false, '9007199254740993'], nested: {n: 7}}, null, 2);
    const p = await page(raw);
    await p.call('saveValue()');
    assert.equal(p.writes[0].body, raw);
});

test('edited numeric JSON is validated but sent verbatim', async () => {
    const p = await page('{"number":1}');
    const edited = '{ "long2":9007199254740995, "text":"edited" }';
    p.elements.jsonEditor.value = edited;
    await p.call('saveValue()');
    assert.equal(p.writes[0].body, edited);
});

test('invalid response JSON does not bind an editable record', async () => {
    const p = await page('not valid JSON');
    await p.call('saveValue()');
    assert.equal(p.elements.jsonEditor.value, '');
    assert.equal(p.elements.jsonEditor.disabled, true);
    assert.equal(p.writes.length, 0);
    assert.ok(p.alerts.some(message => message.startsWith('加载值失败:')));
});

test('invalid edited JSON is still rejected before PUT', async () => {
    const p = await page('{"number":1}');
    p.elements.jsonEditor.value = '{"broken":';
    await p.call('saveValue()');
    assert.equal(p.writes.length, 0);
    assert.ok(p.alerts.some(message => message.includes('无效的JSON格式')));
});
