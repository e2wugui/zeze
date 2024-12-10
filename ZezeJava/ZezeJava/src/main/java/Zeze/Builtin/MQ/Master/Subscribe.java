// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public class Subscribe extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BSubscribe.Data, Zeze.Builtin.MQ.Master.BMQServers.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = -1754779275; // 2540188021
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47418979135861
    static { register(TypeId_, Subscribe.class); }

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

    public Subscribe() {
        Argument = new Zeze.Builtin.MQ.Master.BSubscribe.Data();
        Result = new Zeze.Builtin.MQ.Master.BMQServers.Data();
    }

    public Subscribe(Zeze.Builtin.MQ.Master.BSubscribe.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.MQ.Master.BMQServers.Data();
    }
}
