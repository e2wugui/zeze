// auto-generated
package Game.Fight;

import Zeze.Serialize.*;

public final class tfighters extends Zeze.Transaction.TableX<Game.Fight.BFighterId, Game.Fight.BFighter> {
    public tfighters() {
        super("Game_Fight_tfighters");
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
    public final static int VAR_Attack = 1;
    public final static int VAR_Defence = 2;

    @Override
    public Game.Fight.BFighterId DecodeKey(ByteBuffer _os_) {
        Game.Fight.BFighterId _v_ = new Game.Fight.BFighterId();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Game.Fight.BFighterId _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate();
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Game.Fight.BFighter NewValue() {
        return new Game.Fight.BFighter();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorChanged();
                default: return null;
            }
        }


}
