// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tGameOlineTimer extends Zeze.Transaction.TableX<String, Zeze.Builtin.Timer.BGameOnlineTimer> {
    public tGameOlineTimer() {
        super("Zeze_Builtin_Timer_tGameOlineTimer");
    }

    @Override
    public int getId() {
        return -848362429;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_RoleId = 1;
    public static final int VAR_TimerObj = 2;
    public static final int VAR_LoginVersion = 3;

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
    public Zeze.Builtin.Timer.BGameOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BGameOnlineTimer();
    }
}
