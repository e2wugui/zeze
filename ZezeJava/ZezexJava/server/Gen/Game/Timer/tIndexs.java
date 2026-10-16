// auto-generated @formatter:off
package Game.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// key is timerId
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tIndexs extends TableX<Long, Game.Timer.BIndex>
        implements TableReadOnly<Long, Game.Timer.BIndex, Game.Timer.BIndexReadOnly> {
    public tIndexs() {
        super(1028110618, "Game_Timer_tIndexs");
    }

    public tIndexs(String _s_) {
        super(1028110618, "Game_Timer_tIndexs", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Timer.BIndex> getValueClass() {
        return Game.Timer.BIndex.class;
    }

    public static final int VAR_ServerId = 1;
    public static final int VAR_NodeId = 2;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public Game.Timer.BIndex newValue() {
        return new Game.Timer.BIndex();
    }

    @Override
    public @Nullable BIndexReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
