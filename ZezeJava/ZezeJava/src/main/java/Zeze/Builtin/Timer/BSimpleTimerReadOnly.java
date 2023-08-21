// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BSimpleTimerReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSimpleTimer copy();

    long getDelay();
    long getPeriod();
    long getRemainTimes();
    long getHappenTimes();
    long getStartTime();
    long getEndTime();
    long getNextExpectedTime();
    long getExpectedTime();
    long getHappenTime();
    int getMissfirePolicy();
    String getOneByOneKey();
}
