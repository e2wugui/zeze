// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BDailyTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BDailyTask copy();

    public int getEverydayTaskCount();
    public long getFlushTime();
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getTodayTaskPhaseIdsReadOnly();
}
