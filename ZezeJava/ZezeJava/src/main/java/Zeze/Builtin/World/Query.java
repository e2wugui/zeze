// auto-generated @formatter:off
package Zeze.Builtin.World;

public class Query extends Zeze.Net.Rpc<Zeze.Builtin.World.BCommand.Data, Zeze.Builtin.World.BCommand.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = -448915198; // 3846052098
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47381630294274
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
        Argument = new Zeze.Builtin.World.BCommand.Data();
        Result = new Zeze.Builtin.World.BCommand.Data();
    }

    public Query(Zeze.Builtin.World.BCommand.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.World.BCommand.Data();
    }
}
