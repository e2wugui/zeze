


declare module 'csharp' {
    // 拷贝下面的定义到 puerts 的 d.ts 里面，用来绑定到 c#(cxx?) 的代码
    namespace Zeze {
        type CallbackOnSocketHandshakeDone = (sessionId: number) => void;
        type CallbackOnSocketClose = (sessionId: number) => void;
        type CallbackOnSocketProcessInputBuffer = (sessionId: number, buffer: ArrayBuffer, offset: number, len: number) => void;

        class ToTypeScriptService extends System.Object {
            public constructor(name: string, cb1: CallbackOnSocketHandshakeDone, cb2: CallbackOnSocketClose, cb3: CallbackOnSocketProcessInputBuffer);
            public Connect(hostNameOrAddress: string, port: number, autoReconnect: boolean): void;
            public Send(sessionId: number, buffer: ArrayBuffer, offset: number, len: number): void;
            public Close(sessionId: number): void
            // 把网络事件通知到当前ts中。如果支持其他线程往ts发送消息，再改成通告方式。
            public TickUpdate(): void;
        }
    }
}