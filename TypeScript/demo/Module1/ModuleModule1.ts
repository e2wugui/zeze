/* eslint-disable camelcase, class-methods-use-this, import/no-cycle, new-cap, no-unused-vars, no-useless-constructor, prettier/prettier */
// ZEZE_FILE_CHUNK {{{ IMPORT GEN
import { Zeze } from '../../Zeze/zeze';
import { demo_Module1_Protocol3, demo_Module1_Protocol4, demo_Module1_Rpc1, demo_Module1_Rpc2, demo_Module1_RpcOnlyData, demo_Module1_RpcOnlyUseData } from '../gen';
import App from '../App';
// ZEZE_FILE_CHUNK }}} IMPORT GEN

export default class ModuleModule1 {
    // ZEZE_FILE_CHUNK {{{ MODULE ENUMS
    // ZEZE_FILE_CHUNK }}} MODULE ENUMS
    public constructor(app: App) {
        // ZEZE_FILE_CHUNK {{{ REGISTER PROTOCOL
        app.TestClient.FactoryHandleMap.set(7815467220n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_Protocol3(); },
            p => this.ProcessProtocol3(<demo_Module1_Protocol3>p)));
        app.TestClient.FactoryHandleMap.set(5222864529n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_Protocol4(); },
            p => this.ProcessProtocol4(<demo_Module1_Protocol4>p)));
        app.TestClient.FactoryHandleMap.set(5635082623n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_Rpc1(); },
            null));
        app.TestClient.FactoryHandleMap.set(7854078040n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_Rpc2(); },
            p => this.ProcessRpc2Request(<demo_Module1_Rpc2>p)));
        app.TestClient.FactoryHandleMap.set(7336162357n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_RpcOnlyData(); },
            null));
        app.TestClient.FactoryHandleMap.set(7938190671n, new Zeze.ProtocolFactoryHandle(
            () => { return new demo_Module1_RpcOnlyUseData(); },
            null));
        // ZEZE_FILE_CHUNK }}} REGISTER PROTOCOL
    }

    public Start(app: App): void {
    }

    public Stop(app: App): void {
    }

    public ProcessProtocol4(protocol: demo_Module1_Protocol4): number {
        return 0;
    }

    public ProcessProtocol3(protocol: demo_Module1_Protocol3): number {
        return 0;
    }

    public ProcessRpc2Request(rpc: demo_Module1_Rpc2): number {
        return 0;
    }
}
