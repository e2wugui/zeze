// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public interface BReadWriteLockReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BReadWriteLock copy();

    int getReadingCount();
    boolean isInWriting();
}
