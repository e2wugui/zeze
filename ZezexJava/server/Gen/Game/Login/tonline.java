// auto-generated
package Game.Login;

import Zeze.Serialize.*;

public final class tonline extends Zeze.Transaction.TableX<Long, Game.Login.BOnline> {
    public tonline() {
        super("Game_Login_tonline");
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
    public final static int VAR_LinkName = 1;
    public final static int VAR_LinkSid = 2;
    public final static int VAR_State = 3;
    public final static int VAR_ReliableNotifyMark = 4;
    public final static int VAR_ReliableNotifyQueue = 5;
    public final static int VAR_ReliableNotifyConfirmCount = 6;
    public final static int VAR_ReliableNotifyTotalCount = 7;
    public final static int VAR_ProviderId = 8;
    public final static int VAR_ProviderSessionId = 9;

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
    public Game.Login.BOnline NewValue() {
        return new Game.Login.BOnline();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 3: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 4: return new Zeze.Transaction.ChangeVariableCollectorSet();
            case 5: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 6: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 7: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 8: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 9: return new Zeze.Transaction.ChangeVariableCollectorChanged();
                default: return null;
            }
        }


}
