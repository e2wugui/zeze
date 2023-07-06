// auto-generated @formatter:off
package Zeze.Builtin.World;

// 抽象的交互协议，用来封装任意客户端服务器Protocol,Rpc
public interface BCommandReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommand copy();

    int getCommandId();
    Zeze.Net.Binary getParam();
}
