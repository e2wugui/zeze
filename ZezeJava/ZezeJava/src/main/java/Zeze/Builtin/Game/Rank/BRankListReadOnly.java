// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

public interface BRankListReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BRankList copy();

    public Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.Rank.BRankValue, Zeze.Builtin.Game.Rank.BRankValueReadOnly> getRankListReadOnly();
}
