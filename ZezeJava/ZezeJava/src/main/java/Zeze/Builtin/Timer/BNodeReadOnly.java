// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 若干个timer打包的节点
public interface BNodeReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BNode copy();

    long getPrevNodeId();
    long getNextNodeId();
    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BTimer, Zeze.Builtin.Timer.BTimerReadOnly> getTimersReadOnly();
}
