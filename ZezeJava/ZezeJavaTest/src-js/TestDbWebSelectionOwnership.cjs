'use strict';

// Run with: node --test ZezeJava/ZezeJavaTest/src-js/TestDbWebSelectionOwnership.cjs
// Real production handlers with deferred fetch replies; no timers or network ordering assumptions.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test: runTest} = require('node:test');
const test = (name, action) => runTest(name, {timeout: 5000}, action);

const htmlPath = path.resolve(__dirname, '../../ZezeJava/src/main/resources/DbWebIndex.html');
const html = fs.readFileSync(htmlPath, 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const settle = () => new Promise(resolve => setImmediate(resolve));

class Select {
    options = [];
    selectedIndex = -1;
    add(option) {
        this.options.push(option);
        if (this.selectedIndex < 0) this.selectedIndex = 0;
    }
    set innerHTML(value) {
        assert.equal(value, '');
        this.options = [];
        this.selectedIndex = -1;
    }
    get value() { return this.options[this.selectedIndex]?.value ?? ''; }
    set value(value) {
        let index = this.options.findIndex(option => option.value === value);
        if (index < 0) { index = this.options.length; this.add({text: value, value}); }
        this.selectedIndex = index;
    }
}

async function page() {
    const elements = {
        tableList: new Select(), keyList: new Select(),
        jsonEditor: {value: '', disabled: false}, nextPage: {disabled: false}
    };
    const requests = [], alerts = [];
    const context = vm.createContext({
        URLSearchParams, location: {search: ''},
        localStorage: {getItem: () => 'test-token', setItem: () => {}},
        document: {getElementById: id => elements[id]},
        Option: function(text, value) { this.text = text; this.value = value; },
        alert: message => alerts.push(message), confirm: () => true, prompt: () => null,
        fetch: (url, options) => new Promise(resolve => {
            requests.push({url: new URL(url, 'http://localhost'), options, used: false,
                reply: (data, status = 200) => resolve({ok: status < 400, status, json: async () => data})});
        })
    });
    const call = expression => vm.runInContext(expression, context);
    const take = endpoint => {
        const request = requests.find(r => !r.used && r.url.pathname.endsWith('/' + endpoint));
        assert.ok(request, 'expected pending ' + endpoint);
        request.used = true;
        return request;
    };
    vm.runInContext(script, context, {filename: htmlPath});
    take('ListTable').reply([]);
    await settle();
    return {elements, requests, alerts, call, take,
        select: (table, key) => { elements.tableList.value = table; elements.keyList.value = key; },
        edit: value => {
            assert.equal(elements.jsonEditor.disabled, false);
            elements.jsonEditor.value = value;
            const event = html.match(/<textarea\b[^>]*\boninput="([^"]*)"/);
            if (event) call(event[1]);
        },
        value: () => JSON.parse(elements.jsonEditor.value),
        keys: () => elements.keyList.options.map(option => option.value)};
}

async function loaded(p, table, key, data) {
    p.select(table, key);
    const task = p.call('loadValue()');
    p.take('GetValue').reply(data);
    await task;
}

test('late value reply cannot overwrite the current selection or its saved value', async () => {
    const p = await page();
    p.select('table', 'A');
    const oldTask = p.call('loadValue()'), old = p.take('GetValue');
    p.select('table', 'B');
    const currentTask = p.call('loadValue()'), current = p.take('GetValue');
    current.reply({owner: 'B'}); await currentTask;
    old.reply({owner: 'A'}); await oldTask;
    const save = p.call('saveValue()'), put = p.take('PutRecord');
    assert.equal(put.url.searchParams.get('k'), 'B');
    assert.equal(JSON.parse(put.options.body).owner, 'B');
    put.reply({}); await save;
    assert.equal(p.value().owner, 'B');
});

test('a changed key invalidates the old editor before the new value arrives', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    p.select('table', 'B');
    const task = p.call('loadValue()'), get = p.take('GetValue');
    const save = p.call('saveValue()');
    assert.equal(p.requests.filter(r => r.url.pathname.endsWith('/PutRecord')).length, 0);
    await save;
    const deletion = p.call('deleteKey()');
    assert.equal(p.requests.filter(r => r.url.pathname.endsWith('/DeleteRecord')).length, 0);
    await deletion;
    assert.equal(p.elements.jsonEditor.value, '');
    assert.equal(p.elements.jsonEditor.disabled, true);
    get.reply({owner: 'B'}); await task;
});

test('a changed table invalidates both old keys and the editor while keys are loading', async () => {
    const p = await page();
    await loaded(p, 'A', 'a', {owner: 'A'});
    p.elements.tableList.value = 'B';
    const task = p.call('loadKeys()'), keys = p.take('WalkTable');
    const save = p.call('saveValue()');
    assert.equal(p.requests.filter(r => /\/(PutRecord|DeleteRecord)$/.test(r.url.pathname)).length, 0);
    await save;
    const deletion = p.call('deleteKey()');
    assert.equal(p.requests.filter(r => /\/(PutRecord|DeleteRecord)$/.test(r.url.pathname)).length, 0);
    await deletion;
    assert.deepEqual(p.keys(), []);
    keys.reply({keys: ['b'], hasMore: false}); await task;
    const get = p.take('GetValue');
    assert.equal(get.url.searchParams.get('t'), 'B'); assert.equal(get.url.searchParams.get('k'), 'b');
    get.reply({owner: 'B'}); await settle();
});

test('returning to the same key still rejects a reply from the earlier selection generation', async () => {
    const p = await page();
    p.select('table', 'A'); const firstTask = p.call('loadValue()'), first = p.take('GetValue');
    p.select('table', 'B'); const middleTask = p.call('loadValue()'), middle = p.take('GetValue');
    p.select('table', 'A'); const latestTask = p.call('loadValue()'), latest = p.take('GetValue');
    latest.reply({version: 3}); await latestTask;
    middle.reply({version: 2}); await middleTask;
    first.reply({version: 1}); await firstTask;
    assert.equal(p.value().version, 3);
});

test('late key replies neither replace the current table keys nor start a wrong value read', async () => {
    const p = await page();
    p.elements.tableList.value = 'A'; const oldTask = p.call('loadKeys()'), old = p.take('WalkTable');
    p.elements.tableList.value = 'B'; const task = p.call('loadKeys()'), current = p.take('WalkTable');
    current.reply({keys: ['b'], hasMore: false}); await task;
    const get = p.take('GetValue'); get.reply({owner: 'B'});
    await settle();
    old.reply({keys: ['a'], hasMore: true}); await oldTask;
    assert.deepEqual(p.keys(), ['b']);
    assert.equal(p.requests.filter(r => r.url.pathname.endsWith('/GetValue')).length, 1);
    assert.equal(p.elements.nextPage.disabled, true);
});

test('late next-page replies cannot append old keys or clear the new table editor', async () => {
    const p = await page();
    p.elements.tableList.value = 'A'; const firstTask = p.call('loadKeys()');
    p.take('WalkTable').reply({keys: ['a'], hasMore: true}); await firstTask;
    p.take('GetValue').reply({owner: 'A'}); await settle();
    const oldTask = p.call('nextPage()'), old = p.take('WalkTable');
    assert.equal(old.url.searchParams.get('k'), 'a');
    p.elements.tableList.value = 'B'; const task = p.call('loadKeys()');
    p.take('WalkTable').reply({keys: ['b'], hasMore: false}); await task;
    p.take('GetValue').reply({owner: 'B'}); await settle();
    old.reply({keys: ['a2'], hasMore: false}); await oldTask;
    assert.deepEqual(p.keys(), ['b']);
    assert.equal(p.value().owner, 'B');
});

test('delete completion cannot reset a newly selected record', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    const deletion = p.call('deleteKey()'), request = p.take('DeleteRecord');
    await loaded(p, 'other', 'B', {owner: 'B'});
    request.reply({}); await deletion;
    assert.equal(p.value().owner, 'B');
    assert.equal(p.requests.filter(r => r.url.pathname.endsWith('/WalkTable')).length, 0);
});

test('a completed page append preserves edits to the selected record', async () => {
    const p = await page();
    p.elements.tableList.value = 'table'; const task = p.call('loadKeys()');
    p.take('WalkTable').reply({keys: ['a'], hasMore: true}); await task;
    p.take('GetValue').reply({owner: 'A'}); await settle();
    p.edit('{"unsaved":42}');
    const next = p.call('nextPage()'); p.take('WalkTable').reply({keys: ['b'], hasMore: false}); await next;
    assert.equal(p.value().unsaved, 42);
});

test('delete completion preserves edits made while that request was in flight', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    const deletion = p.call('deleteKey()'), request = p.take('DeleteRecord');
    p.edit('{"unsaved":42}');
    request.reply({}); await deletion;
    assert.equal(p.value().unsaved, 42);
    assert.equal(p.requests.filter(r => r.url.pathname.endsWith('/WalkTable')).length, 0);
});

test('save completion does not claim that a newer selection was saved', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    const save = p.call('saveValue()'), put = p.take('PutRecord');
    await loaded(p, 'table', 'B', {owner: 'B'});
    put.reply({}); await save;
    assert.equal(p.value().owner, 'B');
    assert.equal(p.alerts.includes('保存成功'), false);
});

test('save completion does not claim that edits made during that request were saved', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    const save = p.call('saveValue()'), put = p.take('PutRecord');
    p.edit('{"newerEdit":42}');
    put.reply({}); await save;
    assert.equal(p.value().newerEdit, 42);
    assert.equal(p.alerts.includes('保存成功'), false);
});

test('normal save and delete still act on the loaded tuple and refresh it', async () => {
    const p = await page();
    await loaded(p, 'table', 'A', {owner: 'A'});
    p.elements.jsonEditor.value = '{"edited":42}';
    const save = p.call('saveValue()'), put = p.take('PutRecord');
    assert.equal(put.url.searchParams.get('t'), 'table');
    assert.equal(put.url.searchParams.get('k'), 'A');
    assert.equal(JSON.parse(put.options.body).edited, 42);
    put.reply({}); await save;
    const deletion = p.call('deleteKey()'), request = p.take('DeleteRecord');
    request.reply({}); await deletion;
    p.take('WalkTable').reply({keys: [], hasMore: false}); await settle();
    assert.equal(p.elements.jsonEditor.value, '');
});
