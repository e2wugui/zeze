// auto-generated @formatter:off
package Zeze.Builtin.Timer;

// 使用cron表达式触发时间的timer
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
    long getHappenTimes();
}
