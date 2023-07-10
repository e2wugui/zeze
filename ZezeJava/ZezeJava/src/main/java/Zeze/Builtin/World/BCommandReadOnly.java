// auto-generated @formatter:off
package Zeze.Builtin.World;

// 一个具体的操作。
public interface BCommandReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommand copy();

    int getCommandId();
    Zeze.Net.Binary getParam();
}
