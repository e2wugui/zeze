// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionNPCTalkEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionNPCTalkEvent copy();

    boolean isFinished();
    String getDialogId();
    int getDialogOption();
}
