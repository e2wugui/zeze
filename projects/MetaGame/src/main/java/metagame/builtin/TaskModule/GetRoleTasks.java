// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public class GetRoleTasks extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, metagame.builtin.TaskModule.BGetRoleTasksResult> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = -1388790308; // 2906176988
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42969759006172
    static { register(TypeId_, GetRoleTasks.class); }

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

    public GetRoleTasks() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = new metagame.builtin.TaskModule.BGetRoleTasksResult();
    }
}
