// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTPhaseCommitNPCTalkReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTPhaseCommitNPCTalk copy();

    public long getNpcId();
    public boolean isCommitted();
}
