// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class CloseExchange extends Zeze.Net.Rpc<Zeze.Builtin.Web.BCloseExchange, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 536969102;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47683263889294

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CloseExchange() {
        Argument = new Zeze.Builtin.Web.BCloseExchange();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
