// auto-generated @formatter:off
package ClientGame.Equip;

public class ReportLogin extends Zeze.Net.Rpc<ClientGame.Equip.BReportLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 7;
    public static final int ProtocolId_ = 2130663942;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 32195435014
    static { register(TypeId_, ReportLogin.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public ReportLogin() {
        Argument = new ClientGame.Equip.BReportLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReportLogin(ClientGame.Equip.BReportLogin arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
