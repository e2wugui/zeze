// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Equip.BEquipExtraReadOnly;
// 会重建bean
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tMemoryRetreat extends TableX<Long, Game.Equip.BEquipExtra>
        implements TableReadOnly<Long, Game.Equip.BEquipExtra, Game.Equip.BEquipExtraReadOnly> {
    public tMemoryRetreat() {
        super(1256608074, "Game_Equip_tMemoryRetreat");
    }

    public tMemoryRetreat(String _s_) {
        super(1256608074, "Game_Equip_tMemoryRetreat", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Equip.BEquipExtra> getValueClass() {
        return Game.Equip.BEquipExtra.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_Attack = 1;
    public static final int VAR_Defence = 2;
    public static final int VAR_HotRemoveVar = 3;

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
    public Game.Equip.BEquipExtra newValue() {
        return new Game.Equip.BEquipExtra();
    }

    @Override
    public @Nullable BEquipExtraReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
