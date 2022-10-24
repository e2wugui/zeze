// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BOnlinesReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOnlines copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BOnline, Zeze.Builtin.Online.BOnlineReadOnly> getLoginsReadOnly();
}
