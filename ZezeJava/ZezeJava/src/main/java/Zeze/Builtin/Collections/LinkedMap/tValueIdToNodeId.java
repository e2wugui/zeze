// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tValueIdToNodeId extends Zeze.Transaction.TableX<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeIdReadOnly> {
    public tValueIdToNodeId() {
        super("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId");
    }

    @Override
    public int getId() {
        return -1128401683;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_NodeId = 1;

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId newValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId();
    }

    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeIdReadOnly getReadOnly(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey k) {
        return get(k);
    }
}
