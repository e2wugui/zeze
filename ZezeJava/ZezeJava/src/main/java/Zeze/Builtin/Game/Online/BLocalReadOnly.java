// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public interface BLocalReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLocal copy();

    long getLoginVersion();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Online.BAny, Zeze.Builtin.Game.Online.BAnyReadOnly> getDatasReadOnly();
    Zeze.Builtin.Game.Online.BLink getLink();
}
