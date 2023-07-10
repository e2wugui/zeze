// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BOperateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOperate copy();

    Zeze.Builtin.World.BObjectId getObjectId();
    int getOperateId();
    Zeze.Net.Binary getParam();
}
