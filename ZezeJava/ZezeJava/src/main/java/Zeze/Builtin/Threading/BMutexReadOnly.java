// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BMutexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMutex copy();

    Zeze.Builtin.Threading.BLockName getLockName();
    int getTimeoutMs();
}
