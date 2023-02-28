// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：NPC对话
public interface BTConditionNPCTalkReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionNPCTalk copy();

    long getNpcId();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogOptionsReadOnly();
    Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getDialogSelectedReadOnly();
}
