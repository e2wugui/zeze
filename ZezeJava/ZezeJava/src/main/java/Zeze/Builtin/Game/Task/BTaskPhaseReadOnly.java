// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

// TaskPhase的Bean数据，只存在在BTask之内
public interface BTaskPhaseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskPhase copy();

    public String getTaskPhaseName();
    public String getCurrentConditionName();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Game.Task.BTaskCondition, Zeze.Builtin.Game.Task.BTaskConditionReadOnly> getTaskConditionsReadOnly();
    public Zeze.Transaction.DynamicBeanReadOnly getTaskPhaseCustomDataReadOnly();

}
