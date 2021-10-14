// auto-generated
package Game.Login;

public class ReliableNotifyConfirm extends Zeze.Net.Rpc<Game.Login.BReliableNotifyConfirm, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 14949;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReliableNotifyConfirm() {
        Argument = new Game.Login.BReliableNotifyConfirm();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
