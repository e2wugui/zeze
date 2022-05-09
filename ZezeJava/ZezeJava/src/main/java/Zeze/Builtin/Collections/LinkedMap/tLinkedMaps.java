// auto-generated @formatter:off
package Zeze.Builtin.Collections.LinkedMap;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tLinkedMaps extends Zeze.Transaction.TableX<String, Zeze.Builtin.Collections.LinkedMap.BLinkedMap> {
    public tLinkedMaps() {
        super("Zeze_Builtin_Collections_LinkedMap_tLinkedMaps");
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
    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;
    public static final int VAR_Count = 3;
    public static final int VAR_LastNodeId = 4;

    @Override
    public String DecodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Collections.LinkedMap.BLinkedMap NewValue() {
        return new Zeze.Builtin.Collections.LinkedMap.BLinkedMap();
    }
}
