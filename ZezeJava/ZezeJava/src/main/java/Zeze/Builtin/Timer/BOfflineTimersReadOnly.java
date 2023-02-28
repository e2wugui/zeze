// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BOfflineTimersReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BOfflineTimers copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<String, Integer> getOfflineTimersReadOnly();
}
