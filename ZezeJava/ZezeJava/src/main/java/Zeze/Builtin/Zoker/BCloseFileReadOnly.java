// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public interface BCloseFileReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCloseFile copy();

    String getFileName();
    Zeze.Net.Binary getMd5();
}
