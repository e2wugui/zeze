// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tAccountTimers extends TableX<String, Zeze.Builtin.Timer.BArchOnlineTimer>
        implements TableReadOnly<String, Zeze.Builtin.Timer.BArchOnlineTimer, Zeze.Builtin.Timer.BArchOnlineTimerReadOnly> {
    public tAccountTimers() {
        super(1803422289, "Zeze_Builtin_Timer_tAccountTimers");
    }

    public tAccountTimers(String suffix) {
        super(1803422289, "Zeze_Builtin_Timer_tAccountTimers", suffix);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BArchOnlineTimer> getValueClass() {
        return Zeze.Builtin.Timer.BArchOnlineTimer.class;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    public static final int VAR_Account = 1;
    public static final int VAR_ClientId = 2;
    public static final int VAR_TimerObj = 3;
    public static final int VAR_LoginVersion = 4;
    public static final int VAR_SerialId = 5;

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
    public Zeze.Builtin.Timer.BArchOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BArchOnlineTimer();
    }

    @Override
    public Zeze.Builtin.Timer.BArchOnlineTimerReadOnly getReadOnly(String key) {
        return get(key);
    }
}
