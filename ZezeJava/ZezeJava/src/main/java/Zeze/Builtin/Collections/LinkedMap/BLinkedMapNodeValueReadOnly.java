// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BLinkedMapNodeValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLinkedMapNodeValue copy();

    String getId();
    Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();
}
