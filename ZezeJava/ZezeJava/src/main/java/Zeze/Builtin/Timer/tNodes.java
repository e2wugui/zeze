// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodes extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Timer.BNode, Zeze.Builtin.Timer.BNodeReadOnly> {
    public tNodes() {
        super("Zeze_Builtin_Timer_tNodes");
    }

    @Override
    public int getId() {
        return 453698467;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_PrevNodeId = 1;
    public static final int VAR_NextNodeId = 2;
    public static final int VAR_Timers = 3;

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
    public Zeze.Builtin.Timer.BNode newValue() {
        return new Zeze.Builtin.Timer.BNode();
    }

    @Override
    public Zeze.Builtin.Timer.BNodeReadOnly getReadOnly(Long k) {
        return get(k);
    }
}
