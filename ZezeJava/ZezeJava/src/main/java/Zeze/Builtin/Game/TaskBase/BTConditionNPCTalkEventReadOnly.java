// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionNPCTalkEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionNPCTalkEvent copy();

    public boolean isFinished();
    public String getDialogId();
    public int getDialogOption();
}
