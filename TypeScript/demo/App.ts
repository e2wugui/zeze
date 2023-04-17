// ZEZE_FILE_CHUNK {{{ IMPORT GEN
import { Zeze } from 'zeze/zeze';
import ModuleModule1 from 'demo/Module1/ModuleModule1';
import ModuleModule11 from 'demo/Module1/Module11/ModuleModule11';
// ZEZE_FILE_CHUNK }}} IMPORT GEN

export default class App {
    // ZEZE_FILE_CHUNK {{{ PROPERTY GEN
    public moduleModule1: ModuleModule1;
    public moduleModule11: ModuleModule11;
    public Client: Zeze.Service;
    // ZEZE_FILE_CHUNK }}} PROPERTY GEN

    public constructor() {
        // ZEZE_FILE_CHUNK {{{ PROPERTY INIT GEN
        this.Client = new Zeze.Service("Client");
        this.moduleModule1 = new ModuleModule1(this);
        this.moduleModule11 = new ModuleModule11(this);
        // ZEZE_FILE_CHUNK }}} PROPERTY INIT GEN
    }

    public Start(): void {
        // ZEZE_FILE_CHUNK {{{ START MODULE GEN
        this.moduleModule1.Start(this);
        this.moduleModule11.Start(this);
        // ZEZE_FILE_CHUNK }}} START MODULE GEN
    }

    public Stop(): void {
        // ZEZE_FILE_CHUNK {{{ STOP MODULE GEN
        this.moduleModule1.Stop(this);
        this.moduleModule11.Stop(this);
        // ZEZE_FILE_CHUNK }}} STOP MODULE GEN
    }
}
