// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 这个Bean作为Online.Local.Any存储
public interface BOnlineTimersReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOnlineTimers copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Timer.BOnlineCustom, Zeze.Builtin.Timer.BOnlineCustomReadOnly> getTimerIdsReadOnly();
}
