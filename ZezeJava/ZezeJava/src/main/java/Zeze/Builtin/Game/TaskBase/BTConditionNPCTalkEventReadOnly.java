// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionNPCTalkEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionNPCTalkEvent copy();

    public long getTaskId();
    public long getPhaseId();
    public boolean isFinished();
    public long getDialogId();
    public int getDialogOption();
}
