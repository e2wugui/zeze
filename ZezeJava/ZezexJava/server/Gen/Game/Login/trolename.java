// auto-generated @formatter:off
package Game.Login;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Login.BRoleIdReadOnly;
// rolename to roleid. duplicate name check
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class trolename extends TableX<String, Game.Login.BRoleId>
        implements TableReadOnly<String, Game.Login.BRoleId, Game.Login.BRoleIdReadOnly> {
    public trolename() {
        super(1359458819, "Game_Login_trolename");
    }

    public trolename(String _s_) {
        super(1359458819, "Game_Login_trolename", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Game.Login.BRoleId> getValueClass() {
        return Game.Login.BRoleId.class;
    }

    public static final int VAR_Id = 1;

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
    public Game.Login.BRoleId newValue() {
        return new Game.Login.BRoleId();
    }

    @Override
    public @Nullable BRoleIdReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
