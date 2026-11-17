'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

test('a loaded empty string key can be deleted', async () => {
    const htmlPath = path.resolve(__dirname, '../../ZezeJava/src/main/resources/DbWebIndex.html');
    const script = fs.readFileSync(htmlPath, 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1];
    const elements = {
        tableList: {value: 'stringTable'}, keyList: {value: '', selectedIndex: 0},
        jsonEditor: {value: '', disabled: true}, nextPage: {disabled: true}
    };
    const requests = [], alerts = [];
    const context = vm.createContext({
        URLSearchParams, location: {search: ''},
        localStorage: {getItem: () => '', setItem: () => {}},
        document: {getElementById: id => elements[id]},
        alert: message => alerts.push(message), confirm: () => true,
        fetch: async (url, options) => {
            requests.push({url: new URL(url, 'http://localhost'), options});
            return {ok: true, status: 200, text: async () => '{}',
                json: async () => url.includes('ListTable') ? [] : {keys: [], hasMore: false}};
        }
    });
    vm.runInContext(script, context, {filename: htmlPath});
    await new Promise(resolve => setImmediate(resolve));
    await vm.runInContext('loadValue()', context);
    await vm.runInContext('deleteKey()', context);
    const deletion = requests.find(request => request.options?.method === 'Delete');
    assert.ok(deletion, 'the selected empty key must reach DeleteRecord');
    assert.equal(deletion.url.searchParams.get('t'), 'stringTable');
    assert.equal(deletion.url.searchParams.has('k'), true);
    assert.equal(deletion.url.searchParams.get('k'), '');
    assert.ok(alerts.includes('Key deleted successfully.'));
});
