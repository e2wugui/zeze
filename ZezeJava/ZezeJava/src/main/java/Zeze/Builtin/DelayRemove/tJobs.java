// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tJobs extends TableX<String, Zeze.Builtin.DelayRemove.BJob>
        implements TableReadOnly<String, Zeze.Builtin.DelayRemove.BJob, Zeze.Builtin.DelayRemove.BJobReadOnly> {
    public tJobs() {
        super("Zeze_Builtin_DelayRemove_tJobs");
    }

    @Override
    public int getId() {
        return -582299608;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_JobHandleName = 1;
    public static final int VAR_JobState = 2;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.DelayRemove.BJob newValue() {
        return new Zeze.Builtin.DelayRemove.BJob();
    }

    @Override
    public Zeze.Builtin.DelayRemove.BJobReadOnly getReadOnly(String key) {
        return get(key);
    }
}
