// auto-generated @formatter:off
package demo.Game;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tRole extends TableX<Long, demo.Game.BRole>
        implements TableReadOnly<Long, demo.Game.BRole, demo.Game.BRoleReadOnly> {
    public tRole() {
        super(-346746253, "demo_Game_tRole");
    }

    public tRole(String _s_) {
        super(-346746253, "demo_Game_tRole", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<demo.Game.BRole> getValueClass() {
        return demo.Game.BRole.class;
    }

    public static final int VAR_Level = 1;
    public static final int VAR_Exp = 2;
    public static final int VAR_Money = 3;
    public static final int VAR_Hp = 4;
    public static final int VAR_DamageTotal = 5;
    public static final int VAR_Bag = 6;
    public static final int VAR_Equip = 7;
    public static final int VAR_Friends = 8;

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
    public demo.Game.BRole newValue() {
        return new demo.Game.BRole();
    }

    @Override
    public @Nullable BRoleReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
