// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

// Task rpc的Bean
public interface BTaskEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTaskEvent copy();

    public String getTaskName();
    public Zeze.Transaction.DynamicBeanReadOnly getDynamicDataReadOnly();

}
