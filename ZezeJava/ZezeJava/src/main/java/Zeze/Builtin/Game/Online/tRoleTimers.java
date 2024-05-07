// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tRoleTimers extends TableX<String, Zeze.Builtin.Timer.BGameOnlineTimer>
        implements TableReadOnly<String, Zeze.Builtin.Timer.BGameOnlineTimer, Zeze.Builtin.Timer.BGameOnlineTimerReadOnly> {
    public tRoleTimers() {
        super(1198700620, "Zeze_Builtin_Game_Online_tRoleTimers");
    }

    public tRoleTimers(String suffix) {
        super(1198700620, "Zeze_Builtin_Game_Online_tRoleTimers", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BGameOnlineTimer> getValueClass() {
        return Zeze.Builtin.Timer.BGameOnlineTimer.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_RoleId = 1;
    public static final int VAR_TimerObj = 2;
    public static final int VAR_LoginVersion = 3;
    public static final int VAR_SerialId = 4;

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
    public String decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        String _v_;
        _v_ = rs.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, String _v_) {
        st.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BGameOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BGameOnlineTimer();
    }

    @Override
    public Zeze.Builtin.Timer.BGameOnlineTimerReadOnly getReadOnly(String key) {
        return get(key);
    }
}
