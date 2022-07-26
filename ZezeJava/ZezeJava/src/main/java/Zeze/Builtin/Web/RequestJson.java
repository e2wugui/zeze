// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class RequestJson extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequestJson, Zeze.Builtin.Web.BHttpResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = -1806723945; // 2488243351
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47685215163543

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public RequestJson() {
        Argument = new Zeze.Builtin.Web.BRequestJson();
        Result = new Zeze.Builtin.Web.BHttpResponse();
    }
}
