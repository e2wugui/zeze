// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionKillMonsterEventReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionKillMonsterEvent copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersReadOnly();
}
