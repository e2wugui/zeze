// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BMutexTryLockReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMutexTryLock copy();

    Zeze.Builtin.Threading.BLockName getLockName();
    long getTimeoutMs();
}
