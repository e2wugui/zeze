// auto-generated @formatter:off
package Zeze.Builtin.World;

public interface BAoiOperateReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAoiOperate copy();

    Zeze.Builtin.World.BObjectId getObjectId();
    int getOperateId();
    Zeze.Net.Binary getParam();
}
