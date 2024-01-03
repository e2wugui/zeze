// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public class Accept extends Zeze.Net.Rpc<Zeze.Builtin.Game.TaskModule.BTaskId, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = 309623707;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47322259291035
    static { register(TypeId_, Accept.class); }

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

    public Accept() {
        Argument = new Zeze.Builtin.Game.TaskModule.BTaskId();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Accept(Zeze.Builtin.Game.TaskModule.BTaskId arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
