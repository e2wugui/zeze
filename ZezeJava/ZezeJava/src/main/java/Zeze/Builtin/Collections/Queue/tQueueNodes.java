// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tQueueNodes extends Zeze.Transaction.TableX<Zeze.Builtin.Collections.Queue.BQueueNodeKey, Zeze.Builtin.Collections.Queue.BQueueNode> {
    public tQueueNodes() {
        super("Zeze_Builtin_Collections_Queue_tQueueNodes");
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_NextNodeId = 1;
    public static final int VAR_Values = 2;

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNodeKey DecodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_ = new Zeze.Builtin.Collections.Queue.BQueueNodeKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Builtin.Collections.Queue.BQueueNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.Queue.BQueueNode NewValue() {
        return new Zeze.Builtin.Collections.Queue.BQueueNode();
    }
}
