// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tGameOlineTimer extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Timer.BGameOnlineTimer> {
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

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BGameOnlineTimer newValue() {
        return new Zeze.Builtin.Timer.BGameOnlineTimer();
    }
}
