// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

// Zeze.Transaction.Database.Operates 实现协议
public interface BSetInUseReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSetInUse copy();

    int getLocalId();
    String getGlobal();
}
