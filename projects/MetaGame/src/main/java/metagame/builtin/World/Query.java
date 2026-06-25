// auto-generated @formatter:off
package metagame.builtin.World;

public class Query extends Zeze.Net.Rpc<metagame.builtin.World.BCommand.Data, metagame.builtin.World.BCommand.Data> {
    public static final int ModuleId_ = 10002;
    public static final int ProtocolId_ = 2065433813;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42960328328405
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
        Argument = new metagame.builtin.World.BCommand.Data();
        Result = new metagame.builtin.World.BCommand.Data();
    }

    public Query(metagame.builtin.World.BCommand.Data arg) {
        Argument = arg;
        Result = new metagame.builtin.World.BCommand.Data();
    }
}
