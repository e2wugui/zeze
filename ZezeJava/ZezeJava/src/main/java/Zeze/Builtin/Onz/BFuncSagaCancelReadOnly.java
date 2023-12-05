// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public interface BFuncSagaCancelReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFuncSagaCancel copy();

    long getOnzTid();
    boolean isCancel();
}
