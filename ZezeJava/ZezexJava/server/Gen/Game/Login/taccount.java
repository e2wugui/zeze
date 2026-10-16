// auto-generated @formatter:off
package Game.Login;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class taccount extends TableX<String, Game.Login.BAccount>
        implements TableReadOnly<String, Game.Login.BAccount, Game.Login.BAccountReadOnly> {
    public taccount() {
        super(-177404977, "Game_Login_taccount");
    }

    public taccount(String _s_) {
        super(-177404977, "Game_Login_taccount", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Game.Login.BAccount> getValueClass() {
        return Game.Login.BAccount.class;
    }

    public static final int VAR_Roles = 1;
    public static final int VAR_LastLoginRoleId = 2;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public String decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        String _v_;
        _v_ = _s_.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, String _v_) {
        _s_.appendString("__key", _v_);
    }

    @Override
    public Game.Login.BAccount newValue() {
        return new Game.Login.BAccount();
    }

    @Override
    public @Nullable BAccountReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
