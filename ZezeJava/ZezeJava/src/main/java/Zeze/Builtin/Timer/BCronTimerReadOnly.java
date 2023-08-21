// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BCronTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCronTimer copy();

    String getCronExpression();
    long getNextExpectedTime();
    long getExpectedTime();
    long getHappenTime();
    long getRemainTimes();
    long getEndTime();
    int getMissfirePolicy();
    String getOneByOneKey();
}
