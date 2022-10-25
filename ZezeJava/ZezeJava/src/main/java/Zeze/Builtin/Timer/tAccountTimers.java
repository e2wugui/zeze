// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAccountTimers extends Zeze.Transaction.TableX<String, Zeze.Builtin.Timer.BArchOnlineTimer, Zeze.Builtin.Timer.BArchOnlineTimerReadOnly> {
    public tAccountTimers() {
        super("Zeze_Builtin_Timer_tAccountTimers");
    }

    @Override
    public int getId() {
        return 1803422289;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Account = 1;
    public static final int VAR_ClientId = 2;
    public static final int VAR_TimerObj = 3;
    public static final int VAR_LoginVersion = 4;

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
    public Zeze.Builtin.Timer.BArchOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BArchOnlineTimer();
    }

    @Override
    public Zeze.Builtin.Timer.BArchOnlineTimerReadOnly getReadOnly(String k) {
        return get(k);
    }
}
