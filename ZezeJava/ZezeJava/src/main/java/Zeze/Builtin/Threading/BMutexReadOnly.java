// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BMutexReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BMutex copy();

    boolean isLocked();
    int getServerId();
    long getLockTime();
}
