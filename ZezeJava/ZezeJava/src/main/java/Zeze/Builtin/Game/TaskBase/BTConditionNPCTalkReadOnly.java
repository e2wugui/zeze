// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：NPC对话
public interface BTConditionNPCTalkReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionNPCTalk copy();

    public long getNpcId();
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogOptionsReadOnly();
    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogSelectedReadOnly();
}
