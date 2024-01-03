// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public class Finish extends Zeze.Net.Rpc<Zeze.Builtin.Game.TaskModule.BTaskId, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = -1437472689; // 2857494607
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47324807161935
    static { register(TypeId_, Finish.class); }

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

    public Finish() {
        Argument = new Zeze.Builtin.Game.TaskModule.BTaskId();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Finish(Zeze.Builtin.Game.TaskModule.BTaskId arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
