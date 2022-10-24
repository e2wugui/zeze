// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public interface BQueueTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BQueueTask copy();

    public String getQueueName();
    public int getTaskType();
    public long getTaskId();
    public Zeze.Net.Binary getTaskParam();
    public long getPrevTaskId();
}
