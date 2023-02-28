// auto-generated @formatter:off
package Zeze.Builtin.RedoQueue;

public interface BQueueTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BQueueTask copy();

    String getQueueName();
    int getTaskType();
    long getTaskId();
    Zeze.Net.Binary getTaskParam();
    long getPrevTaskId();
}
