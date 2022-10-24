// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BOfflineTimersReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BOfflineTimers copy();

    public Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly();
}
