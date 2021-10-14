// auto-generated
package Game.Equip;

import Zeze.Serialize.*;

public final class tequip extends Zeze.Transaction.TableX<Long, Game.Equip.BEquips> {
    public tequip() {
        super("Game_Equip_tequip");
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
    public final static int VAR_Items = 1;

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
    public Game.Equip.BEquips NewValue() {
        return new Game.Equip.BEquips();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorMap(() -> new Zeze.Transaction.ChangeNoteMap2<Integer, Game.Bag.BItem>(null));
                default: return null;
            }
        }


}
