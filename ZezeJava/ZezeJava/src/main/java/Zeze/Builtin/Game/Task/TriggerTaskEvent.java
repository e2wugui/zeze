// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

public class TriggerTaskEvent extends Zeze.Net.Rpc<Zeze.Builtin.Game.Task.BTaskEvent, Zeze.Builtin.Game.Task.BTaskEventResult> {
    public static final int ModuleId_ = 11018;
    public static final int ProtocolId_ = -1350190119; // 2944777177
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47324894444505

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public TriggerTaskEvent() {
        Argument = new Zeze.Builtin.Game.Task.BTaskEvent();
        Result = new Zeze.Builtin.Game.Task.BTaskEventResult();
    }

    public TriggerTaskEvent(Zeze.Builtin.Game.Task.BTaskEvent arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Game.Task.BTaskEventResult();
    }
}
