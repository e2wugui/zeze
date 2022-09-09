// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class RequestInputStream extends Zeze.Net.Rpc<Zeze.Builtin.Web.BStream, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 1906817333;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47684633737525

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public RequestInputStream() {
        Argument = new Zeze.Builtin.Web.BStream();
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
