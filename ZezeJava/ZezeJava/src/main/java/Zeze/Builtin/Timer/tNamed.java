// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNamed extends Zeze.Transaction.TableX<String, Zeze.Builtin.Timer.BTimerId> {
    public tNamed() {
        super("Zeze_Builtin_Timer_tNamed");
    }

    @Override
    public int getId() {
        return -1980975417;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_TimerId = 1;

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
    public Zeze.Builtin.Timer.BTimerId newValue() {
        return new Zeze.Builtin.Timer.BTimerId();
    }
}
