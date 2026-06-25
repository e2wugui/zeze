// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public class TaskRemoved extends Zeze.Net.Protocol<metagame.builtin.TaskModule.BTaskId> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = 1784082001;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42968636911185
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
        Argument = new metagame.builtin.TaskModule.BTaskId();
    }

    public TaskRemoved(metagame.builtin.TaskModule.BTaskId arg) {
        Argument = arg;
    }
}
