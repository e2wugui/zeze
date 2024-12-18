// auto-generated @formatter:off
package Zeze.Builtin.MQ;

public interface BSendMessageReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSendMessage copy();

    String getTopic();
    int getPartitionIndex();
    Zeze.Builtin.MQ.BMessageReadOnly getMessageReadOnly();
}
