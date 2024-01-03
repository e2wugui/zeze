// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public class GetRoleTasks extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Builtin.Game.TaskModule.BGetRoleTasksResult> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = -135503844; // 4159463452
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47326109130780
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
        Result = new Zeze.Builtin.Game.TaskModule.BGetRoleTasksResult();
    }
}
