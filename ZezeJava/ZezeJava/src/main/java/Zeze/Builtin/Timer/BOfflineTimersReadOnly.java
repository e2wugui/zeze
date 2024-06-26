// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 记录多个定时器(timerId)的反向索引
public interface BOfflineTimersReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOfflineTimers copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly();
}
