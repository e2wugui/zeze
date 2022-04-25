// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tValueIdToNodeId extends Zeze.Transaction.TableX<Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey, Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId> {
    public tValueIdToNodeId() {
        super("Zeze_Builtin_Collections_LinkedMap_tValueIdToNodeId");
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_All = 0;
    public static final int VAR_NodeId = 1;

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey DecodeKey(ByteBuffer _os_) {
        Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_ = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId NewValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeId();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch (variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            default: return null;
        }
    }
}
