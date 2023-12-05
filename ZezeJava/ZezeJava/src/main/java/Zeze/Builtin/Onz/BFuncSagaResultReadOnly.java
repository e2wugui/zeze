// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public interface BFuncSagaResultReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFuncSagaResult copy();

    Zeze.Net.Binary getFuncResult();
}
