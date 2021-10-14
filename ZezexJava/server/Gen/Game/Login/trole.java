// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class trole extends Zeze.Transaction.TableX<Long, Game.Login.BRoleData> {
    public trole() {
        super("Game_Login_trole");
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return true;
    }

    public final static int VAR_All = 0;
    public final static int VAR_Name = 1;

    public long Insert(Game.Login.BRoleData value) {
            long key = getAutoKey().Next();
            Insert(key, value);
            return key;
    }

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
    public Game.Login.BRoleData NewValue() {
        return new Game.Login.BRoleData();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
                default: return null;
            }
        }


}
