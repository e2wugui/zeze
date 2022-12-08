// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：击杀怪物
public interface BTConditionKillMonsterReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTConditionKillMonster copy();

    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersReadOnly();
    public Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersKilledReadOnly();
}
