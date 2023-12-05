// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public interface BFuncProcedureReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BFuncProcedure copy();

    long getOnzTid();
    String getFuncName();
    Zeze.Net.Binary getFuncArgument();
}
