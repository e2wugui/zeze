// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

// 工作目录的第一级子目录既是
public class StartService extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BStartService.Data, Zeze.Builtin.Zoker.BService.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -1628045811; // 2666921485
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47406220967437
    static { register(TypeId_, StartService.class); }

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

    public StartService() {
        Argument = new Zeze.Builtin.Zoker.BStartService.Data();
        Result = new Zeze.Builtin.Zoker.BService.Data();
    }

    public StartService(Zeze.Builtin.Zoker.BStartService.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BService.Data();
    }
}
