// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BSemaphoreReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSemaphore copy();

    Zeze.Builtin.Threading.BLockName getLockName();
    int getPermits();
    int getTimeoutMs();
}
