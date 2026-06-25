// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public class Accept extends Zeze.Net.Rpc<metagame.builtin.TaskModule.BTaskId, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = -301823088; // 3993144208
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42970845973392
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
        Argument = new metagame.builtin.TaskModule.BTaskId();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Accept(metagame.builtin.TaskModule.BTaskId arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
