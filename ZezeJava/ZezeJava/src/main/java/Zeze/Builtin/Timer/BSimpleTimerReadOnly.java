// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BSimpleTimerReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BSimpleTimer copy();

    public long getDelay();
    public long getPeriod();
    public long getRemainTimes();
    public long getHappenTimes();
    public long getStartTime();
    public long getEndTime();
    public long getNextExpectedTime();
    public long getExpectedTime();
    public long getHappenTime();
    public int getMissfirePolicy();
}
