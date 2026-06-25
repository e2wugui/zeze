// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public class Finish extends Zeze.Net.Rpc<metagame.builtin.TaskModule.BTaskId, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = -1173686830; // 3121280466
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42969974109650
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
        Argument = new metagame.builtin.TaskModule.BTaskId();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Finish(metagame.builtin.TaskModule.BTaskId arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
