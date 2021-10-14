// auto-generated
package Game.Bag;

import Zeze.Serialize.*;

public final class tbag extends Zeze.Transaction.TableX<Long, Game.Bag.BBag> {
    public tbag() {
        super("Game_Bag_tbag");
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public final static int VAR_All = 0;
    public final static int VAR_Money = 1;
    public final static int VAR_Capacity = 2;
    public final static int VAR_Items = 3;

    @Override
    public Long DecodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate();
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Game.Bag.BBag NewValue() {
        return new Game.Bag.BBag();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 3: return new Zeze.Transaction.ChangeVariableCollectorMap(() -> new Zeze.Transaction.ChangeNoteMap2<Integer, Game.Bag.BItem>(null));
                default: return null;
            }
        }


}
