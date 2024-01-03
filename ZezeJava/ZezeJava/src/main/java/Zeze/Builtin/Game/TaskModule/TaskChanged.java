// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskModule;

public class TaskChanged extends Zeze.Net.Protocol<Zeze.Builtin.Game.TaskModule.BTaskDescription> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = -116392023; // 4178575273
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47326128242601
    static { register(TypeId_, TaskChanged.class); }

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

    public TaskChanged() {
        Argument = new Zeze.Builtin.Game.TaskModule.BTaskDescription();
    }

    public TaskChanged(Zeze.Builtin.Game.TaskModule.BTaskDescription arg) {
        Argument = arg;
    }
}
