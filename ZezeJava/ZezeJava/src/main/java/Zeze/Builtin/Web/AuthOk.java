// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class AuthOk extends Zeze.Net.Rpc<Zeze.Builtin.Web.BAuthOk, Zeze.Builtin.Web.BResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 267396600;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47682994316792

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AuthOk() {
        Argument = new Zeze.Builtin.Web.BAuthOk();
        Result = new Zeze.Builtin.Web.BResponse();
    }
}
