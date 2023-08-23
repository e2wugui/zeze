// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BOnlinesReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnlines copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BOnline, Zeze.Builtin.Online.BOnlineReadOnly> getLoginsReadOnly();
    long getLastLoginVersion();
    String getAccount();
}
