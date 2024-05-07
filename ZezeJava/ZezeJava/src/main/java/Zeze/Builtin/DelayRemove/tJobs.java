// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tJobs extends TableX<Integer, Zeze.Builtin.DelayRemove.BJobs>
        implements TableReadOnly<Integer, Zeze.Builtin.DelayRemove.BJobs, Zeze.Builtin.DelayRemove.BJobsReadOnly> {
    public tJobs() {
        super(-582299608, "Zeze_Builtin_DelayRemove_tJobs");
    }

    public tJobs(String suffix) {
        super(-582299608, "Zeze_Builtin_DelayRemove_tJobs", suffix);
    }

    @Override
    public Class<Integer> getKeyClass() {
        return Integer.class;
    }

    @Override
    public Class<Zeze.Builtin.DelayRemove.BJobs> getValueClass() {
        return Zeze.Builtin.DelayRemove.BJobs.class;
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
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Integer decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        int _v_;
        _v_ = rs.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Integer _v_) {
        st.appendInt("__key", _v_);
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
