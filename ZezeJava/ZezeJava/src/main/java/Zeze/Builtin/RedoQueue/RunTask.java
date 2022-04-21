// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public class RunTask extends Zeze.Net.Rpc<Zeze.Builtin.RedoQueue.BQueueTask, Zeze.Builtin.RedoQueue.BTaskId> {
    public static final int ModuleId_ = 11010;
    public static final int ProtocolId_ = 1530872255;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public RunTask() {
        Argument = new Zeze.Builtin.RedoQueue.BQueueTask();
        Result = new Zeze.Builtin.RedoQueue.BTaskId();
    }
}
