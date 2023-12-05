// auto-generated @formatter:off
package Zeze.Builtin.Onz;

// saga confirm
public interface BFuncSagaReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFuncSaga copy();

    long getOnzTid();
    String getFuncName();
    Zeze.Net.Binary getFuncArgument();
}
