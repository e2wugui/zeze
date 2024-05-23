// auto-generated @formatter:off
package Zeze.Builtin.LinksInfo;

public interface BLinkInfoReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLinkInfo copy();

    String getIp();
    int getPort();
    Zeze.Net.Binary getExtra();
}
