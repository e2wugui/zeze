// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BMessageReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMessage copy();

    String getTopic();
    int getPartitionIndex();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, String> getPropertiesReadOnly();
    Zeze.Net.Binary getBody();
    long getTimestamp();
}
