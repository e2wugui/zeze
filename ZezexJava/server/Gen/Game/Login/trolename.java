// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class trolename extends Zeze.Transaction.TableX<String, Game.Login.BRoleId> {
    public trolename() {
        super("Game_Login_trolename");
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
    public final static int VAR_Id = 1;

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
    public Game.Login.BRoleId NewValue() {
        return new Game.Login.BRoleId();
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
