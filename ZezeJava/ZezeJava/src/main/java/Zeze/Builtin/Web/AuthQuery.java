// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class AuthQuery extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequestQuery, Zeze.Builtin.Web.BHttpResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = 1402089079;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47684129009271

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public AuthQuery() {
        Argument = new Zeze.Builtin.Web.BRequestQuery();
        Result = new Zeze.Builtin.Web.BHttpResponse();
    }
}
