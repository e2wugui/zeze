// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tIndexs extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Timer.BIndex> {
    public tIndexs() {
        super("Zeze_Builtin_Timer_tIndexs");
    }

    @Override
    public int getId() {
        return 833718;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_ServerId = 1;
    public static final int VAR_NodeId = 2;
    public static final int VAR_NamedName = 3;

    @Override
    public Long DecodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Timer.BIndex NewValue() {
        return new Zeze.Builtin.Timer.BIndex();
    }
}
