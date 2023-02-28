// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BDailyTaskReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BDailyTask copy();

    int getEverydayTaskCount();
    long getFlushTime();
    Zeze.Transaction.Collections.PList1ReadOnly<Long> getTodayTaskPhaseIdsReadOnly();
}
