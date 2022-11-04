// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public class CompleteCondition extends Zeze.Net.Rpc<Zeze.Builtin.Game.Task.BCompleteCondition, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = 1439906754;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47323389574082

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CompleteCondition() {
        Argument = new Zeze.Builtin.Game.Task.BCompleteCondition();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public CompleteCondition(Zeze.Builtin.Game.Task.BCompleteCondition arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
