// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BNodeReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BNode copy();

    public long getPrevNodeId();
    public long getNextNodeId();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BTimer, Zeze.Builtin.Timer.BTimerReadOnly> getTimersReadOnly();
}
