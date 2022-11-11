// auto-generated @formatter:off
package Zeze.Builtin.Game.Task;

// Task rpc
public interface BTaskEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskEvent copy();

    public String getTaskName();
    public long getTaskPhaseId();
    public long getTaskConditionId();
    public Zeze.Transaction.DynamicBeanReadOnly getDynamicDataReadOnly();

}
