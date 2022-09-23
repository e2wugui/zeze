// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tRoleOfflineTimers extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Timer.BOfflineTimers> {
    public tRoleOfflineTimers() {
        super("Zeze_Builtin_Timer_tRoleOfflineTimers");
    }

    @Override
    public int getId() {
        return 987199499;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_OfflineTimers = 1;

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
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }
}
