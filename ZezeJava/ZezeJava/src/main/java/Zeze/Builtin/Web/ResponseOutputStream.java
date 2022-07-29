// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class ResponseOutputStream extends Zeze.Net.Rpc<Zeze.Builtin.Web.BStream, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 766458906;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47683493379098

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ResponseOutputStream() {
        Argument = new Zeze.Builtin.Web.BStream();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
