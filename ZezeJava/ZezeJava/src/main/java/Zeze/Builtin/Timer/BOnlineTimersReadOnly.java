// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 这个Bean作为Online.Local.Any存储
public interface BOnlineTimersReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOnlineTimers copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BOnlineCustom, Zeze.Builtin.Timer.BOnlineCustomReadOnly> getTimerIdsReadOnly();
}
