// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tequip extends TableX<Long, Game.Equip.BEquips>
        implements TableReadOnly<Long, Game.Equip.BEquips, Game.Equip.BEquipsReadOnly> {
    public tequip() {
        super(264042677, "Game_Equip_tequip");
    }

    public tequip(String _s_) {
        super(264042677, "Game_Equip_tequip", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Equip.BEquips> getValueClass() {
        return Game.Equip.BEquips.class;
    }

    public static final int VAR_Items = 1;

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
    public Game.Equip.BEquips newValue() {
        return new Game.Equip.BEquips();
    }

    @Override
    public @Nullable BEquipsReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
