// auto-generated @formatter:off
package Game.Equip;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Equip.BRelationalReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tRelationalTest extends TableX<Long, Game.Equip.BRelational>
        implements TableReadOnly<Long, Game.Equip.BRelational, Game.Equip.BRelationalReadOnly> {
    public tRelationalTest() {
        super(1462248996, "Game_Equip_tRelationalTest");
    }

    public tRelationalTest(String _s_) {
        super(1462248996, "Game_Equip_tRelationalTest", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Equip.BRelational> getValueClass() {
        return Game.Equip.BRelational.class;
    }

    @Override
    public boolean isRelationalMapping() {
        return true;
    }

    public static final int VAR_Id = 1;

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
    public Game.Equip.BRelational newValue() {
        return new Game.Equip.BRelational();
    }

    @Override
    public @Nullable BRelationalReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
