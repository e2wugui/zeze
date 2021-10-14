// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class taccount extends Zeze.Transaction.TableX<String, Game.Login.BAccount> {
    public taccount() {
        super("Game_Login_taccount");
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
    public final static int VAR_Name = 1;
    public final static int VAR_Roles = 2;
    public final static int VAR_LastLoginRoleId = 3;

    @Override
    public String DecodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate();
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Game.Login.BAccount NewValue() {
        return new Game.Login.BAccount();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 3: return new Zeze.Transaction.ChangeVariableCollectorChanged();
                default: return null;
            }
        }


}
