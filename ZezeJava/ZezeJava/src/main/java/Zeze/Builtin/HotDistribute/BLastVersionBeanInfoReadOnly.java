// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public interface BLastVersionBeanInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLastVersionBeanInfo copy();

    String getName();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.HotDistribute.BVariable, Zeze.Builtin.HotDistribute.BVariableReadOnly> getVariablesReadOnly();
}
