// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public interface BAppendFileReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAppendFile copy();

    String getServiceName();
    String getFileName();
    long getOffset();
    Zeze.Net.Binary getChunk();
}
