// auto-generated @formatter:off
package metagame.builtin.TaskModule;

// 这里分解成Accepted等明确操作更灵活，但这样应该足够了。
public class TaskChanged extends Zeze.Net.Protocol<metagame.builtin.TaskModule.BTaskDescription> {
    public static final int ModuleId_ = 10004;
    public static final int ProtocolId_ = -2045096382; // 2249870914
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42969102700098
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
        Argument = new metagame.builtin.TaskModule.BTaskDescription();
    }

    public TaskChanged(metagame.builtin.TaskModule.BTaskDescription arg) {
        Argument = arg;
    }
}
