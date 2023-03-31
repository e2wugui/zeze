// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public class RunTask extends Zeze.Net.Rpc<Zeze.Builtin.RedoQueue.BQueueTask, Zeze.Builtin.RedoQueue.BTaskId> {
    public static final int ModuleId_ = 11010;
    public static final int ProtocolId_ = 1530872255;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47289120801215
    static { register(TypeId_, RunTask.class); }

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

    public RunTask() {
        Argument = new Zeze.Builtin.RedoQueue.BQueueTask();
        Result = new Zeze.Builtin.RedoQueue.BTaskId();
    }

    public RunTask(Zeze.Builtin.RedoQueue.BQueueTask arg) {
        Argument = arg;
        Result = new Zeze.Builtin.RedoQueue.BTaskId();
    }
}
