// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BTConditionKillMonsterEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionKillMonsterEvent copy();

    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersReadOnly();
}
