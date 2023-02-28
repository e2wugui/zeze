// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BAnyReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAny copy();

    Zeze.Transaction.DynamicBeanReadOnly getAnyReadOnly();
}
