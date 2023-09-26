// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class Browse extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSession.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -1565958308; // 2729008988
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47397693120348
    static { register(TypeId_, Browse.class); }

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

    public Browse() {
        Argument = new Zeze.Builtin.LogService.BSession.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public Browse(Zeze.Builtin.LogService.BSession.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
