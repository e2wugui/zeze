// auto-generated @formatter:off
package Zeze.Builtin.Takeover;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key是serverId，与数据表同库：裁决与搬运同一zeze事务
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tTakeoverLease extends TableX<Integer, Zeze.Builtin.Takeover.BTakeoverLease>
        implements TableReadOnly<Integer, Zeze.Builtin.Takeover.BTakeoverLease, Zeze.Builtin.Takeover.BTakeoverLeaseReadOnly> {
    public tTakeoverLease() {
        super(1920559310, "Zeze_Builtin_Takeover_tTakeoverLease");
    }

    public tTakeoverLease(String _s_) {
        super(1920559310, "Zeze_Builtin_Takeover_tTakeoverLease", _s_);
    }

    @Override
    public Class<Integer> getKeyClass() {
        return Integer.class;
    }

    @Override
    public Class<Zeze.Builtin.Takeover.BTakeoverLease> getValueClass() {
        return Zeze.Builtin.Takeover.BTakeoverLease.class;
    }

    public static final int VAR_Epoch = 1;
    public static final int VAR_ExpireAt = 2;

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
    public Integer decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        int _v_;
        _v_ = _s_.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Integer _v_) {
        _s_.appendInt("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Takeover.BTakeoverLease newValue() {
        return new Zeze.Builtin.Takeover.BTakeoverLease();
    }

    @Override
    public Zeze.Builtin.Takeover.BTakeoverLeaseReadOnly getReadOnly(Integer _k_) {
        return get(_k_);
    }
}
