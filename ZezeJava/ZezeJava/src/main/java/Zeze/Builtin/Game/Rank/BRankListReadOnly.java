// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

public interface BRankListReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRankList copy();

    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.Game.Rank.BRankValue, Zeze.Builtin.Game.Rank.BRankValueReadOnly> getRankListReadOnly();
}
