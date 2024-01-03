// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public class TaskRemoved extends Zeze.Net.Protocol<Zeze.Builtin.Game.TaskModule.BTaskId> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = 232619494;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47322182286822
    static { register(TypeId_, TaskRemoved.class); }

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

    public TaskRemoved() {
        Argument = new Zeze.Builtin.Game.TaskModule.BTaskId();
    }

    public TaskRemoved(Zeze.Builtin.Game.TaskModule.BTaskId arg) {
        Argument = arg;
    }
}
