// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public class TriggerTaskEvent extends Zeze.Net.Rpc<Zeze.Builtin.Game.TaskBase.BTaskEvent, Zeze.Builtin.Game.TaskBase.BTaskEventResult> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = 1070967817;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47323020635145

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public TriggerTaskEvent() {
        Argument = new Zeze.Builtin.Game.TaskBase.BTaskEvent();
        Result = new Zeze.Builtin.Game.TaskBase.BTaskEventResult();
    }

    public TriggerTaskEvent(Zeze.Builtin.Game.TaskBase.BTaskEvent arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Game.TaskBase.BTaskEventResult();
    }
}
