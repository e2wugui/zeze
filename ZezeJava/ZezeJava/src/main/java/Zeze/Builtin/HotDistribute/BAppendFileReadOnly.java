// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public interface BAppendFileReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BAppendFile copy();

    String getFileName();
    long getOffset();
    Zeze.Net.Binary getChunk();
}
