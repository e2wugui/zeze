// auto-generated @formatter:off
package Zeze.Builtin.Token;

public interface BPubTopicReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BPubTopic copy();

    String getTopic();
    Zeze.Net.Binary getContent();
    boolean isBroadcast();
}
