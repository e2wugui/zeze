// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class StopService extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BStopService.Data, Zeze.Builtin.Zoker.BService.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -1586358548; // 2708608748
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47406262654700
    static { register(TypeId_, StopService.class); }

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

    public StopService() {
        Argument = new Zeze.Builtin.Zoker.BStopService.Data();
        Result = new Zeze.Builtin.Zoker.BService.Data();
    }

    public StopService(Zeze.Builtin.Zoker.BStopService.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BService.Data();
    }
}
