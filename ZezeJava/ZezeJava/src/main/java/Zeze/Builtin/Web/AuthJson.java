// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class AuthJson extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequestJson, Zeze.Builtin.Web.BHttpResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 1883946168;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47684610866360

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AuthJson() {
        Argument = new Zeze.Builtin.Web.BRequestJson();
        Result = new Zeze.Builtin.Web.BHttpResponse();
    }
}
