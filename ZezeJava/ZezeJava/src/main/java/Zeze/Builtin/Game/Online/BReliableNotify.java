// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BReliableNotify extends Zeze.Transaction.Bean implements BReliableNotifyReadOnly {
    public static final long TYPEID = -6166834646872658332L;

    private final Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> _Notifies; // full encoded protocol list
    private long _ReliableNotifyIndex; // Notify的计数开始。客户端收到的总计数为：start + Notifies.Count

    public Zeze.Transaction.Collections.PList1<Zeze.Net.Binary> getNotifies() {
        return _Notifies;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Zeze.Net.Binary> getNotifiesReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_Notifies);
    }

    @Override
    public long getReliableNotifyIndex() {
        if (!isManaged())
            return _ReliableNotifyIndex;
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyRead(this);
        if (_t_ == null)
            return _ReliableNotifyIndex;
        var log = (Log__ReliableNotifyIndex)_t_.getLog(objectId() + 2);
        return log != null ? log.value : _ReliableNotifyIndex;
    }

    public void setReliableNotifyIndex(long _v_) {
        if (!isManaged()) {
            _ReliableNotifyIndex = _v_;
            return;
        }
        var _t_ = Zeze.Transaction.Transaction.getCurrentVerifyWrite(this);
        _t_.putLog(new Log__ReliableNotifyIndex(this, 2, _v_));
    }

    @SuppressWarnings("deprecation")
    public BReliableNotify() {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.variableId(1);
    }

    @SuppressWarnings("deprecation")
    public BReliableNotify(long _ReliableNotifyIndex_) {
        _Notifies = new Zeze.Transaction.Collections.PList1<>(Zeze.Net.Binary.class);
        _Notifies.variableId(1);
        _ReliableNotifyIndex = _ReliableNotifyIndex_;
    }

    @Override
    public void reset() {
        _Notifies.clear();
        setReliableNotifyIndex(0);
        _unknown_ = null;
    }

    public void assign(BReliableNotify _o_) {
        _Notifies.assign(_o_._Notifies);
        setReliableNotifyIndex(_o_.getReliableNotifyIndex());
        _unknown_ = _o_._unknown_;
    }

    public BReliableNotify copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BReliableNotify copy() {
        var _c_ = new BReliableNotify();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BReliableNotify _a_, BReliableNotify _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    private static final class Log__ReliableNotifyIndex extends Zeze.Transaction.Logs.LogLong {
        public Log__ReliableNotifyIndex(BReliableNotify _b_, int _i_, long _v_) { super(_b_, _i_, _v_); }

        @Override
        public void commit() { ((BReliableNotify)getBelong())._ReliableNotifyIndex = value; }
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Game.Online.BReliableNotify: {\n");
        _s_.append(_i1_).append("Notifies=[");
        if (!_Notifies.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _Notifies) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("],\n");
        _s_.append(_i1_).append("ReliableNotifyIndex=").append(getReliableNotifyIndex()).append('\n');
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
            var _x_ = _Notifies;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.BYTES);
                for (var _v_ : _x_) {
                    _o_.WriteBinary(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        {
            long _x_ = getReliableNotifyIndex();
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
            var _x_ = _Notifies;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadBinary(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            setReliableNotifyIndex(_o_.ReadLong(_t_));
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BReliableNotify))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BReliableNotify)_o_;
        if (!_Notifies.equals(_b_._Notifies))
            return false;
        if (getReliableNotifyIndex() != _b_.getReliableNotifyIndex())
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _Notifies.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _Notifies.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        if (getReliableNotifyIndex() < 0)
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
                case 1: _Notifies.followerApply(_v_); break;
                case 2: _ReliableNotifyIndex = _v_.longValue(); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_Notifies, Zeze.Net.Binary.class, _r_.getString(_pn_ + "Notifies"));
        setReliableNotifyIndex(_r_.getLong(_pn_ + "ReliableNotifyIndex"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Notifies", Zeze.Serialize.Helper.encodeJson(_Notifies));
        _s_.appendLong(_pn_ + "ReliableNotifyIndex", getReliableNotifyIndex());
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Notifies", "list", "", "binary"));
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ReliableNotifyIndex", "long", "", ""));
        return _v_;
    }
}
