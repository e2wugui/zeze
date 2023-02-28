// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

public interface BJobsReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BJobs copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.DelayRemove.BJob, Zeze.Builtin.DelayRemove.BJobReadOnly> getJobsReadOnly();
}
