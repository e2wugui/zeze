// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BVersionsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BVersions copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BVersion, Zeze.Builtin.Online.BVersionReadOnly> getLoginsReadOnly();
    long getLastLoginVersion();
}
