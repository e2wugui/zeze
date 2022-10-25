// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tIndexs extends Zeze.Transaction.TableX<String, Zeze.Builtin.Timer.BIndex, Zeze.Builtin.Timer.BIndexReadOnly> {
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
    public Zeze.Builtin.Timer.BIndex newValue() {
        return new Zeze.Builtin.Timer.BIndex();
    }

    public Zeze.Builtin.Timer.BIndexReadOnly getReadOnly(String k) {
        return get(k);
    }
}
