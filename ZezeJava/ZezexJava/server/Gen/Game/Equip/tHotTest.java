// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// 新版热更模块修改这个这张表的Value结构定义。
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tHotTest extends TableX<Long, Game.Equip.BEquipExtra>
        implements TableReadOnly<Long, Game.Equip.BEquipExtra, Game.Equip.BEquipExtraReadOnly> {
    public tHotTest() {
        super(-839272234, "Game_Equip_tHotTest");
    }

    public tHotTest(String _s_) {
        super(-839272234, "Game_Equip_tHotTest", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Equip.BEquipExtra> getValueClass() {
        return Game.Equip.BEquipExtra.class;
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
