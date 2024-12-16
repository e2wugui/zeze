// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

public class CreateMQ extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BMQInfo.Data, Zeze.Builtin.MQ.Master.BMQServers.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = -490132214; // 3804835082
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47420243782922
    static { register(TypeId_, CreateMQ.class); }

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

    public CreateMQ() {
        Argument = new Zeze.Builtin.MQ.Master.BMQInfo.Data();
        Result = new Zeze.Builtin.MQ.Master.BMQServers.Data();
    }

    public CreateMQ(Zeze.Builtin.MQ.Master.BMQInfo.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.MQ.Master.BMQServers.Data();
    }
}
