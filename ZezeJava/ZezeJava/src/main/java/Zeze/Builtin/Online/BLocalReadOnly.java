// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BLocalReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLocal copy();

    public long getLoginVersion();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BAny, Zeze.Builtin.Online.BAnyReadOnly> getDatasReadOnly();
}
