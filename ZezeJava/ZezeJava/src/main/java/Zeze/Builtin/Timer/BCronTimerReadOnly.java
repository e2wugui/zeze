// auto-generated @formatter:off
package Zeze.Builtin.Timer;

public interface BCronTimerReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BCronTimer copy();

    public String getCronExpression();
    public long getNextExpectedTime();
    public long getExpectedTime();
    public long getHappenTime();
    public long getRemainTimes();
    public long getEndTime();
    public int getMissfirePolicy();
}
