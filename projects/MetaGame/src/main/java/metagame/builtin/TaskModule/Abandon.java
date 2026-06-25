// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public class Abandon extends Zeze.Net.Rpc<metagame.builtin.TaskModule.BTaskId, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = -609450596; // 3685516700
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42970538345884
    static { register(TypeId_, Abandon.class); }

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

    public Abandon() {
        Argument = new metagame.builtin.TaskModule.BTaskId();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Abandon(metagame.builtin.TaskModule.BTaskId arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
