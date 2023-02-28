// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// 内置条件类型：击杀怪物
public interface BTConditionKillMonsterReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BTConditionKillMonster copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersReadOnly();
    Zeze.Transaction.Collections.PMap1ReadOnly<Long, Integer> getMonstersKilledReadOnly();
}
