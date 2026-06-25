// auto-generated @formatter:off
package metagame.builtin.World;

// 一个具体的操作。
public interface BCommandReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCommand copy();

    long getMapInstanceId();
    int getCommandId();
    Zeze.Net.Binary getParam();
}
