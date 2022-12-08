// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTPhaseCommitNPCTalkEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTPhaseCommitNPCTalkEvent copy();

    public long getPhaseId();
    public long getNpcId();
}
