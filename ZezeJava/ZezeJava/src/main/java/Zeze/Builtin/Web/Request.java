// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class Request extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequest, Zeze.Builtin.Web.BResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = -117897707; // 4177069589
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47686903989781

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Request() {
        Argument = new Zeze.Builtin.Web.BRequest();
        Result = new Zeze.Builtin.Web.BResponse();
    }

    public Request(Zeze.Builtin.Web.BRequest arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Web.BResponse();
    }
}
