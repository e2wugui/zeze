// auto-generated @formatter:off
package Zeze.Beans.RedoQueue;

public class RunTask extends Zeze.Net.Rpc<Zeze.Beans.RedoQueue.BQueueTask, Zeze.Beans.RedoQueue.BTaskId> {
    public static final int ModuleId_ = 11010;
    public static final int ProtocolId_ = 1606216633;
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
        Argument = new Zeze.Beans.RedoQueue.BQueueTask();
        Result = new Zeze.Beans.RedoQueue.BTaskId();
    }
}
