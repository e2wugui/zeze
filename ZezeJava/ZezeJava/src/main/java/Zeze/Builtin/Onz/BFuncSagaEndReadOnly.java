// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public interface BFuncSagaEndReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFuncSagaEnd copy();

    long getOnzTid();
    boolean isCancel();
    Zeze.Net.Binary getFuncArgument();
}
