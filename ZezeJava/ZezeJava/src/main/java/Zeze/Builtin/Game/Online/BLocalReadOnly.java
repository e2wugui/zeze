// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BLocalReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLocal copy();

    public long getLoginVersion();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Online.BAny, Zeze.Builtin.Game.Online.BAnyReadOnly> getDatasReadOnly();
}
