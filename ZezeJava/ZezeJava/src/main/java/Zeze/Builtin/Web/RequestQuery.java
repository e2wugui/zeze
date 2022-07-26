// auto-generated @formatter:off
package Zeze.Builtin.Web;

public class RequestQuery extends Zeze.Net.Rpc<Zeze.Builtin.Web.BRequestQuery, Zeze.Builtin.Web.BHttpResponse> {
    public static final int ModuleId_ = 11102;
    public static final int ProtocolId_ = -311980974; // 3982986322
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47686709906514

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public RequestQuery() {
        Argument = new Zeze.Builtin.Web.BRequestQuery();
        Result = new Zeze.Builtin.Web.BHttpResponse();
    }
}
