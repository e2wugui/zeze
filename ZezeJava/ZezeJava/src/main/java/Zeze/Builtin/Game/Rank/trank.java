// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class trank extends TableX<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList>
        implements TableReadOnly<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList, Zeze.Builtin.Game.Rank.BRankListReadOnly> {
    public trank() {
        super(-2043108039, "Zeze_Builtin_Game_Rank_trank");
    }

    public trank(String _s_) {
        super(-2043108039, "Zeze_Builtin_Game_Rank_trank", _s_);
    }

    @Override
    public Class<Zeze.Builtin.Game.Rank.BConcurrentKey> getKeyClass() {
        return Zeze.Builtin.Game.Rank.BConcurrentKey.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Rank.BRankList> getValueClass() {
        return Zeze.Builtin.Game.Rank.BRankList.class;
    }

    public static final int VAR_RankList = 1;

    @Override
    public Zeze.Builtin.Game.Rank.BConcurrentKey decodeKey(ByteBuffer _os_) {
        return Zeze.Builtin.Game.Rank.BConcurrentKey.readFrom(_os_);
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Game.Rank.BConcurrentKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Rank.BConcurrentKey decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        var _p_ = new java.util.ArrayList<String>();
        Zeze.Builtin.Game.Rank.BConcurrentKey _v_;
        _p_.add("__key");
        try {
            _v_ = Zeze.Builtin.Game.Rank.BConcurrentKey.readFromResultSet(_p_, _s_);
        } finally {
            _p_.removeLast();
        }
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Zeze.Builtin.Game.Rank.BConcurrentKey _v_) {
        var _p_ = new java.util.ArrayList<String>();
        _p_.add("__key");
        _v_.encodeSQLStatement(_p_, _s_);
        _p_.removeLast();
    }

    @Override
    public Zeze.Builtin.Game.Rank.BRankList newValue() {
        return new Zeze.Builtin.Game.Rank.BRankList();
    }

    @Override
    public @Nullable BRankListReadOnly getReadOnly(Zeze.Builtin.Game.Rank.BConcurrentKey _k_) {
        return get(_k_);
    }
}
