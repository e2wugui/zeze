// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReliableNotifyConfirm extends Zeze.Transaction.Bean implements BReliableNotifyConfirmReadOnly {
    public static final long TYPEID = -6588057877320371892L;

    private long _ReliableNotifyConfirmIndex;
    private boolean _Sync;

    @Override
    public long getReliableNotifyConfirmIndex() {
        if (!isManaged())
            return _ReliableNotifyConfirmIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReliableNotifyConfirmIndex;
        var log = (Log__ReliableNotifyConfirmIndex)_t_.getLog(objectId() + 1);
        return log != null ? log.value : _ReliableNotifyConfirmIndex;
    }

    public void setReliableNotifyConfirmIndex(long _v_) {
        if (!isManaged()) {
            _ReliableNotifyConfirmIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ReliableNotifyConfirmIndex(this, 1, _v_));
    }

    @Override
    public boolean isSync() {
        if (!isManaged())
            return _Sync;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _Sync;
        var log = (Log__Sync)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _Sync;
    }

    public void setSync(boolean _v_) {
        if (!isManaged()) {
            _Sync = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__Sync(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm() {
    }

    @SuppressWarnings("deprecation")
    public BReliableNotifyConfirm(long _ReliableNotifyConfirmIndex_, boolean _Sync_) {
        _ReliableNotifyConfirmIndex = _ReliableNotifyConfirmIndex_;
        _Sync = _Sync_;
    }

    @Override
    public void reset() {
        setReliableNotifyConfirmIndex(0);
        setSync(false);
        _unknown_ = null;
    }

    public void assign(BReliableNotifyConfirm _o_) {
        setReliableNotifyConfirmIndex(_o_.getReliableNotifyConfirmIndex());
        setSync(_o_.isSync());
        _unknown_ = _o_._unknown_;
    }

    public BReliableNotifyConfirm copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotifyConfirm copy() {
        var _c_ = new BReliableNotifyConfirm();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReliableNotifyConfirm _a_, BReliableNotifyConfirm _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyConfirmIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyConfirmIndex(BReliableNotifyConfirm _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._ReliableNotifyConfirmIndex = value; }
    }

    private static final class Log__Sync extends Zeze.Transaction.Logs.LogBool {
        public Log__Sync(BReliableNotifyConfirm _b_, int _i_, boolean _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReliableNotifyConfirm)getBelong())._Sync = value; }
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.append(System.lineSeparator()).toString();
    }

    @Override
    public void buildString(StringBuilder _s_, int _l_) {
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Zeze.Builtin.Game.Online.BReliableNotifyConfirm: {").append(System.lineSeparator());
        _l_ += 4;
        _s_.append(Zeze.Util.Str.indent(_l_)).append("ReliableNotifyConfirmIndex=").append(getReliableNotifyConfirmIndex()).append(',').append(System.lineSeparator());
        _s_.append(Zeze.Util.Str.indent(_l_)).append("Sync=").append(isSync()).append(System.lineSeparator());
        _l_ -= 4;
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
            long _x_ = getReliableNotifyConfirmIndex();
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            boolean _x_ = isSync();
            if (_x_) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteByte(1);
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
            setReliableNotifyConfirmIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setSync(_o_.ReadBool(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReliableNotifyConfirm))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReliableNotifyConfirm)_o_;
        if (getReliableNotifyConfirmIndex() != _b_.getReliableNotifyConfirmIndex())
            return false;
        if (isSync() != _b_.isSync())
            return false;
        return true;
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyConfirmIndex() < 0)
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
                case 1: _ReliableNotifyConfirmIndex = _v_.longValue(); break;
                case 2: _Sync = _v_.booleanValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        setReliableNotifyConfirmIndex(_r_.getLong(_pn_ + "ReliableNotifyConfirmIndex"));
        setSync(_r_.getBoolean(_pn_ + "Sync"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendLong(_pn_ + "ReliableNotifyConfirmIndex", getReliableNotifyConfirmIndex());
        _s_.appendBoolean(_pn_ + "Sync", isSync());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ReliableNotifyConfirmIndex", "long", "", ""));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Sync", "bool", "", ""));
        return _v_;
    }
}
