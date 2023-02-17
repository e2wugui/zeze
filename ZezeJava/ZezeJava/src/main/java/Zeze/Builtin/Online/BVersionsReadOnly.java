// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BVersionsReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BVersions copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BVersion, Zeze.Builtin.Online.BVersionReadOnly> getLoginsReadOnly();
    public long getLastLoginVersion();
}
