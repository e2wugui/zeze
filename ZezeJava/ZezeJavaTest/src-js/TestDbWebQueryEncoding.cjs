'use strict';

// Run with: node --test ZezeJava/ZezeJavaTest/src-js/TestDbWebQueryEncoding.cjs
// Executes the production inline script; DOM and fetch are bounded in-memory substitutes.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

const htmlPath = path.resolve(__dirname, '../../ZezeJava/src/main/resources/DbWebIndex.html');
const script = fs.readFileSync(htmlPath, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1];
const token = 'token&#+%中文';

class Select {
    options = [];
    selectedIndex = -1;
    add(option) { this.options.push(option); }
    set innerHTML(value) {
        assert.equal(value, '');
        this.options = [];
        this.selectedIndex = -1;
    }
    get value() { return this.options[this.selectedIndex]?.value ?? ''; }
    set value(value) {
        this.options = [{text: value, value}];
        this.selectedIndex = 0;
    }
}

async function page(replacementToken) {
    const elements = {
        tableList: new Select(), keyList: new Select(),
        jsonEditor: {value: ''}, nextPage: {disabled: false}
    };
    const requests = [];
    const alerts = [];
    const storage = new Map();
    let prompts = 0;
    const context = vm.createContext({
        URLSearchParams,
        location: {search: '?token=' + encodeURIComponent(token)},
        localStorage: {getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value)},
        document: {getElementById: id => assert.ok(elements[id]) || elements[id]},
        Option: function(text, value) { this.text = text; this.value = value; },
        alert: message => alerts.push(message), confirm: () => true,
        prompt: () => {
            if (replacementToken === undefined) return null;
            assert.equal(++prompts, 1, 'an accepted replacement must stop the authentication loop');
            return replacementToken;
        },
        fetch: async (url, options) => {
            requests.push({url: new URL(url, 'http://localhost'), options});
            if (replacementToken !== undefined && new URL(url, 'http://localhost').searchParams.get('token') !== replacementToken)
                return {ok: false, status: 403};
            const data = url.includes('/WalkTable') ? {keys: [], hasMore: false}
                : url.includes('/ListTable') ? [] : {number: 42};
            return {ok: true, status: 200,
                json: async () => data, text: async () => JSON.stringify(data)};
        }
    });
    vm.runInContext(script, context, {filename: htmlPath});
    // init() starts immediately. Drain its JSON continuation before exercising a selection.
    await Promise.resolve();
    await Promise.resolve();
    return {
        elements, requests, alerts, storage,
        call: expression => vm.runInContext(expression, context),
        setCursor: value => vm.runInContext('pagination.startKey = ' + JSON.stringify(value), context)
    };
}

test('ListTable preserves the token from the page URL', async () => {
    const p = await page();
    assert.equal(p.requests.length, 1);
    assert.equal(p.requests[0].url.pathname, '/Zeze/Builtin/DbWeb/ListTable');
    assert.equal(p.requests[0].url.searchParams.get('token'), token);
    assert.equal(p.storage.get('dbweb_token'), token);
});

test('a replacement token supersedes a rejected token in the page URL', async () => {
    const replacement = 'replacement&#+%中文';
    const p = await page(replacement);
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(p.requests.length, 2);
    assert.equal(p.requests[1].url.searchParams.get('token'), replacement);
    assert.equal(p.storage.get('dbweb_token'), replacement);
    assert.equal(p.call('apiUrl(API.value)').includes(encodeURIComponent(token)), false);
    assert.equal(new URL(p.call('apiUrl(API.value)'), 'http://localhost').searchParams.get('token'), replacement);
    assert.deepEqual(p.alerts, []);
});

const cases = ['plain', 'amp&other=wrong', 'hash#tail', 'plus+tail', 'percent%20literal', '中文鍵', '&#+%中文'];
for (const suffix of cases) {
    for (const endpoint of ['WalkTable', 'GetValue', 'PutRecord', 'DeleteRecord']) {
        test(endpoint + ' preserves table and key: ' + suffix, async () => {
            const p = await page();
            const table = 'table_' + suffix;
            const key = 'key_' + suffix;
            p.elements.tableList.value = table;
            p.elements.keyList.value = key;
            // The UI only permits mutations after the selected record has loaded.
            if (endpoint === 'PutRecord' || endpoint === 'DeleteRecord') await p.call('loadValue()');
            const body = '{"number":42}';
            p.elements.jsonEditor.value = body;
            const action = {
                WalkTable: 'loadKeys(false)', GetValue: 'loadValue()',
                PutRecord: 'saveValue()', DeleteRecord: 'deleteKey()'
            }[endpoint];
            p.setCursor(key);
            await p.call(action);
            const request = p.requests.find(r => r.url.pathname.endsWith('/' + endpoint));
            assert.ok(request, 'the real inline handler must call the expected endpoint');
            assert.equal(request.url.hash, '', 'a data character must not become a URL fragment');
            assert.equal(request.url.searchParams.get('t'), table);
            assert.equal(request.url.searchParams.get('k'), key);
            assert.equal(request.url.searchParams.get('token'), token);
            assert.equal(request.url.searchParams.size, endpoint === 'WalkTable' ? 4 : 3,
                'data must not inject query parameters');
            if (endpoint === 'WalkTable') assert.equal(request.url.searchParams.get('n'), '20');
            if (endpoint === 'PutRecord') {
                assert.equal(request.options.method.toLowerCase(), 'put');
                assert.equal(request.options.body, body);
            }
            if (endpoint === 'DeleteRecord') assert.equal(request.options.method.toLowerCase(), 'delete');
        });
    }
}
