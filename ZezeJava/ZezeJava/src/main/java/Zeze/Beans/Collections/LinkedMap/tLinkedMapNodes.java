// auto-generated @formatter:off
package Zeze.Beans.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

public final class tLinkedMapNodes extends Zeze.Transaction.TableX<Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey, Zeze.Beans.Collections.LinkedMap.BLinkedMapNode> {
    public tLinkedMapNodes() {
        super("Zeze_Beans_Collections_LinkedMap_tLinkedMapNodes");
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
    public static final int VAR_PrevNodeId = 1;
    public static final int VAR_NextNodeId = 2;
    public static final int VAR_Values = 3;

    @Override
    public Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey DecodeKey(ByteBuffer _os_) {
        Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey _v_ = new Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Beans.Collections.LinkedMap.BLinkedMapNodeKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Beans.Collections.LinkedMap.BLinkedMapNode NewValue() {
        return new Zeze.Beans.Collections.LinkedMap.BLinkedMapNode();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch (variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 3: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            default: return null;
        }
    }
}
