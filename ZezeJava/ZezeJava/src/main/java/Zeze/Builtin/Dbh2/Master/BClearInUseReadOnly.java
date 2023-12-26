// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public interface BClearInUseReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BClearInUse copy();

    int getLocalId();
    String getGlobal();
}
