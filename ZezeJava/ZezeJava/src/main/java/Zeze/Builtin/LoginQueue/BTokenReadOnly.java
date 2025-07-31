// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

public interface BTokenReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BToken copy();

    int getServerId();
    long getExpireTime();
    long getSerialId();
    int getLinkServerId();
}
