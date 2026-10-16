// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tMail extends TableX<Long, demo.Game.BMail>
        implements TableReadOnly<Long, demo.Game.BMail, demo.Game.BMailReadOnly> {
    public tMail() {
        super(232302076, "demo_Game_tMail");
    }

    public tMail(String _s_) {
        super(232302076, "demo_Game_tMail", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Game.BMail> getValueClass() {
        return demo.Game.BMail.class;
    }

    public static final int VAR_From = 1;
    public static final int VAR_Money = 2;
    public static final int VAR_Items = 3;
    public static final int VAR_Claimed = 4;

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
    public demo.Game.BMail newValue() {
        return new demo.Game.BMail();
    }

    @Override
    public @Nullable BMailReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
