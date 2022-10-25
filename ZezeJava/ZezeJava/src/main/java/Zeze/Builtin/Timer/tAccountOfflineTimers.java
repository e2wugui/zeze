// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAccountOfflineTimers extends Zeze.Transaction.TableX<Zeze.Builtin.Timer.BAccountClientId, Zeze.Builtin.Timer.BOfflineTimers, Zeze.Builtin.Timer.BOfflineTimersReadOnly> {
    public tAccountOfflineTimers() {
        super("Zeze_Builtin_Timer_tAccountOfflineTimers");
    }

    @Override
    public int getId() {
        return -865861330;
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
    public Zeze.Builtin.Timer.BAccountClientId decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Timer.BAccountClientId _v_ = new Zeze.Builtin.Timer.BAccountClientId();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Timer.BAccountClientId _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BOfflineTimers newValue() {
        return new Zeze.Builtin.Timer.BOfflineTimers();
    }

    public Zeze.Builtin.Timer.BOfflineTimersReadOnly getReadOnly(Zeze.Builtin.Timer.BAccountClientId k) {
        return get(k);
    }
}
