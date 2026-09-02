// auto-generated @formatter:off
package Zeze.Builtin.Takeover;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BTakeoverLease extends Zeze.Transaction.Bean implements BTakeoverLeaseReadOnly {
    public static final long TYPEID = -7205020741574480182L;

    private long _Epoch; // 代际号：claim时 old+1，抢占式，重启不等旧租约过期
    private long _ExpireAt; // 毫秒时间戳；owner周期续约；0=墓碑(已被接管或正常关闭)

    private static final java.lang.invoke.VarHandle vh_Epoch;
    private static final java.lang.invoke.VarHandle vh_ExpireAt;

    static {
        var _l_ = java.lang.invoke.MethodHandles.lookup();
        try {
            vh_Epoch = _l_.findVarHandle(BTakeoverLease.class, "_Epoch", long.class);
            vh_ExpireAt = _l_.findVarHandle(BTakeoverLease.class, "_ExpireAt", long.class);
        } catch (ReflectiveOperationException _e_) {
            throw Zeze.Util.Task.forceThrow(_e_);
        }
    }

    @Override
    public long getEpoch() {
        if (!isManaged())
            return _Epoch;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Epoch;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _Epoch;
    }

    public void setEpoch(long _v_) {
        if (!isManaged()) {
            _Epoch = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 1, vh_Epoch, _v_));
    }

    @Override
    public long getExpireAt() {
        if (!isManaged())
            return _ExpireAt;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ExpireAt;
        var log = (Zeze.Transaction.Logs.LogLong)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ExpireAt;
    }

    public void setExpireAt(long _v_) {
        if (!isManaged()) {
            _ExpireAt = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Zeze.Transaction.Logs.LogLong(this, 2, vh_ExpireAt, _v_));
    }

    @SuppressWarnings("deprecation")
    public BTakeoverLease() {
    }

    @SuppressWarnings("deprecation")
    public BTakeoverLease(long _Epoch_, long _ExpireAt_) {
        _Epoch = _Epoch_;
        _ExpireAt = _ExpireAt_;
    }

    @Override
    public void reset() {
        setEpoch(0);
        setExpireAt(0);
        _unknown_ = null;
    }

    public void assign(BTakeoverLease _o_) {
        setEpoch(_o_.getEpoch());
        setExpireAt(_o_.getExpireAt());
        _unknown_ = _o_._unknown_;
    }

    public BTakeoverLease copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BTakeoverLease copy() {
        var _c_ = new BTakeoverLease();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BTakeoverLease _a_, BTakeoverLease _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Takeover.BTakeoverLease: {\n");
        _s_.append(_i1_).append("Epoch=").append(getEpoch()).append(",\n");
        _s_.append(_i1_).append("ExpireAt=").append(getExpireAt()).append('\n');
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    private byte[] _unknown_;

    public byte[] unknown() {
        return _unknown_;
    }

    public void clearUnknown() {
        _unknown_ = null;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        ByteBuffer _u_ = null;
        var _ua_ = _unknown_;
        var _ui_ = _ua_ != null ? (_u_ = ByteBuffer.Wrap(_ua_)).readUnknownIndex() : Long.MAX_VALUE;
        int _i_ = 0;
        {
            long _x_ = getEpoch();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            long _x_ = getExpireAt();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.writeAllUnknownFields(_i_, _ui_, _u_);
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        ByteBuffer _u_ = null;
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            setEpoch(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setExpireAt(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BTakeoverLease))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTakeoverLease)_o_;
        if (getEpoch() != _b_.getEpoch())
            return false;
        if (getExpireAt() != _b_.getExpireAt())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getEpoch() < 0)
            return true;
        if (getExpireAt() < 0)
            return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void followerApply(Zeze.Transaction.Log _l_) {
        var _vs_ = ((Zeze.Transaction.Collections.LogBean)_l_).getVariables();
        if (_vs_ == null)
            return;
        for (var _i_ = _vs_.iterator(); _i_.moveToNext(); ) {
            var _v_ = _i_.value();
            switch (_v_.getVariableId()) {
                case 1: _Epoch = _v_.longValue(); break;
                case 2: _ExpireAt = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setEpoch(_r_.getLong(_pn_ + "Epoch"));
        setExpireAt(_r_.getLong(_pn_ + "ExpireAt"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "Epoch", getEpoch());
        _s_.appendLong(_pn_ + "ExpireAt", getExpireAt());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Epoch", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ExpireAt", "long", "", ""));
        return _v_;
    }
}
