// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

import Zeze.Serialize.ByteBuffer;

public final class tItemClasses extends Zeze.Transaction.TableX<Integer, Zeze.Builtin.Game.Bag.BItemClasses> {
    public tItemClasses() {
        super("Zeze_Builtin_Game_Bag_tItemClasses");
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
    public static final int VAR_ItemClasses = 1;

    @Override
    public Integer DecodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Bag.BItemClasses NewValue() {
        return new Zeze.Builtin.Game.Bag.BItemClasses();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch (variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorSet();
            default: return null;
        }
    }
}
