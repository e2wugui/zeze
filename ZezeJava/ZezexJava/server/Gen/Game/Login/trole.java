// auto-generated @formatter:off
package Game.Login;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Login.BRoleReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class trole extends TableX<Long, Game.Login.BRole>
        implements TableReadOnly<Long, Game.Login.BRole, Game.Login.BRoleReadOnly> {
    public trole() {
        super(239580138, "Game_Login_trole");
    }

    public trole(String _s_) {
        super(239580138, "Game_Login_trole", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Login.BRole> getValueClass() {
        return Game.Login.BRole.class;
    }

    public static final int VAR_Id = 1;
    public static final int VAR_Name = 2;

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
    public Game.Login.BRole newValue() {
        return new Game.Login.BRole();
    }

    @Override
    public @Nullable BRoleReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
