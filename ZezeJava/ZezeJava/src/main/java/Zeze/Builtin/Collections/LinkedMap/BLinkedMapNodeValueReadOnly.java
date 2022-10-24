// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

public interface BLinkedMapNodeValueReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLinkedMapNodeValue copy();

    public String getId();
    public Zeze.Transaction.DynamicBeanReadOnly getValueReadOnly();

}
