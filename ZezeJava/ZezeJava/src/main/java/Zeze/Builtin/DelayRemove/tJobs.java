// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tJobs extends TableX<Integer, Zeze.Builtin.DelayRemove.BJobs>
        implements TableReadOnly<Integer, Zeze.Builtin.DelayRemove.BJobs, Zeze.Builtin.DelayRemove.BJobsReadOnly> {
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

    public static final int VAR_Jobs = 1;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.DelayRemove.BJobs newValue() {
        return new Zeze.Builtin.DelayRemove.BJobs();
    }

    @Override
    public Zeze.Builtin.DelayRemove.BJobsReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
