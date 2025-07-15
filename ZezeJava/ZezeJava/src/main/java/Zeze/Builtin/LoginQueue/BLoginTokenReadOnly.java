// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

public interface BLoginTokenReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLoginToken copy();

    Zeze.Net.Binary getToken();
    String getLinkIp();
    int getLinkPort();
}
