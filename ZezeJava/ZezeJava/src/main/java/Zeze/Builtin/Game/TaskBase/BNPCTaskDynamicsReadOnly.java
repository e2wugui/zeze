// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置任务类型：NPCTask
public interface BNPCTaskDynamicsReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BNPCTaskDynamics copy();

    public long getReceiveNpcId();
    public long getSubmitNpcId();
}
