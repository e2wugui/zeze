// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class Query extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BJson.Data, Zeze.Builtin.LogService.BJson.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = 1393034717;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47396357146077
    static { register(TypeId_, Query.class); }

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

    public Query() {
        Argument = new Zeze.Builtin.LogService.BJson.Data();
        Result = new Zeze.Builtin.LogService.BJson.Data();
    }

    public Query(Zeze.Builtin.LogService.BJson.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BJson.Data();
    }
}
