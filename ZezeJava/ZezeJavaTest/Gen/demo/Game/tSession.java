// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tSession extends TableX<Long, demo.Game.BSession>
        implements TableReadOnly<Long, demo.Game.BSession, demo.Game.BSessionReadOnly> {
    public tSession() {
        super(-1482752374, "demo_Game_tSession");
    }

    public tSession(String _s_) {
        super(-1482752374, "demo_Game_tSession", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Game.BSession> getValueClass() {
        return demo.Game.BSession.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_LastActive = 1;
    public static final int VAR_RoomId = 2;

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
    public demo.Game.BSession newValue() {
        return new demo.Game.BSession();
    }

    @Override
    public @Nullable BSessionReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
