// ZEZE_FILE_CHUNK {{{ IMPORT GEN
import { Zeze } from "zeze/zeze"
import { demo_Module1 } from "demo/Module1/ModuleModule1"
import { demo_Module1_Module11 } from "demo/Module1/Module11/ModuleModule11"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

export class demo_App {
    // ZEZE_FILE_CHUNK {{{ PROPERTY GEN
    public demo_Module1: demo_Module1;
    public demo_Module1_Module11: demo_Module1_Module11;
    public Client: Zeze.Service;
    // ZEZE_FILE_CHUNK }}} PROPERTY GEN

    public constructor() {
        // ZEZE_FILE_CHUNK {{{ PROPERTY INIT GEN
        this.Client = new Zeze.Service("Client");
        this.demo_Module1 = new demo_Module1(this);
        this.demo_Module1_Module11 = new demo_Module1_Module11(this);
        // ZEZE_FILE_CHUNK }}} PROPERTY INIT GEN
    }

    public Start(): void {
        // ZEZE_FILE_CHUNK {{{ START MODULE GEN
        this.demo_Module1.Start(this);
        this.demo_Module1_Module11.Start(this);
        // ZEZE_FILE_CHUNK }}} START MODULE GEN
    }

    public Stop(): void {
        // ZEZE_FILE_CHUNK {{{ STOP MODULE GEN
        this.demo_Module1.Stop(this);
        this.demo_Module1_Module11.Stop(this);
        // ZEZE_FILE_CHUNK }}} STOP MODULE GEN
    }
}
