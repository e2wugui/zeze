// auto-generated @formatter:off
package Zeze.Builtin.Provider;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"EqualsAndHashcode", "NullableProblems", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "SwitchStatementWithTooFewBranches", "UnusedAssignment"})
public final class BSendResult extends Zeze.Transaction.Bean implements BSendResultReadOnly {
    public static final long TYPEID = -7186434891670297524L;

    private final Zeze.Transaction.Collections.PList1<Long> _ErrorLinkSids;

    public Zeze.Transaction.Collections.PList1<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    @Override
    public Zeze.Transaction.Collections.PList1ReadOnly<Long> getErrorLinkSidsReadOnly() {
        return new Zeze.Transaction.Collections.PList1ReadOnly<>(_ErrorLinkSids);
    }

    @SuppressWarnings("deprecation")
    public BSendResult() {
        _ErrorLinkSids = new Zeze.Transaction.Collections.PList1<>(Long.class);
        _ErrorLinkSids.variableId(1);
    }

    @Override
    public void reset() {
        _ErrorLinkSids.clear();
        _unknown_ = null;
    }

    @Override
    public Zeze.Builtin.Provider.BSendResult.Data toData() {
        var _d_ = new Zeze.Builtin.Provider.BSendResult.Data();
        _d_.assign(this);
        return _d_;
    }

    @Override
    public void assign(Zeze.Transaction.Data _o_) {
        assign((Zeze.Builtin.Provider.BSendResult.Data)_o_);
    }

    public void assign(BSendResult.Data _o_) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(_o_._ErrorLinkSids);
        _unknown_ = null;
    }

    public void assign(BSendResult _o_) {
        _ErrorLinkSids.assign(_o_._ErrorLinkSids);
        _unknown_ = _o_._unknown_;
    }

    public BSendResult copyIfManaged() {
        return isManaged() ? copy() : this;
    }

    @Override
    public BSendResult copy() {
        var _c_ = new BSendResult();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSendResult _a_, BSendResult _b_) {
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
        var _i2_ = Zeze.Util.Str.indent(_l_ + 8);
        _s_.append("Zeze.Builtin.Provider.BSendResult: {\n");
        _s_.append(_i1_).append("ErrorLinkSids=[");
        if (!_ErrorLinkSids.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _ErrorLinkSids) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
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
            var _x_ = _ErrorLinkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (var _v_ : _x_) {
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
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
            var _x_ = _ErrorLinkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        //noinspection ConstantValue
        _unknown_ = _o_.readAllUnknownFields(_i_, _t_, _u_);
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSendResult))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSendResult)_o_;
        if (!_ErrorLinkSids.equals(_b_._ErrorLinkSids))
            return false;
        return true;
    }

    @Override
    protected void initChildrenRootInfo(Zeze.Transaction.Record.RootInfo _r_) {
        _ErrorLinkSids.initRootInfo(_r_, this);
    }

    @Override
    protected void initChildrenRootInfoWithRedo(Zeze.Transaction.Record.RootInfo _r_) {
        _ErrorLinkSids.initRootInfoWithRedo(_r_, this);
    }

    @Override
    public boolean negativeCheck() {
        for (var _v_ : _ErrorLinkSids) {
            if (_v_ < 0)
                return true;
        }
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
                case 1: _ErrorLinkSids.followerApply(_v_); break;
            }
        }
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        Zeze.Serialize.Helper.decodeJsonList(_ErrorLinkSids, Long.class, _r_.getString(_pn_ + "ErrorLinkSids"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "ErrorLinkSids", Zeze.Serialize.Helper.encodeJson(_ErrorLinkSids));
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var _v_ = super.variables();
        _v_.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ErrorLinkSids", "list", "", "long"));
        return _v_;
    }

@SuppressWarnings("ForLoopReplaceableByForEach")
public static final class Data extends Zeze.Transaction.Data {
    public static final long TYPEID = -7186434891670297524L;

    private java.util.ArrayList<Long> _ErrorLinkSids;

    public java.util.ArrayList<Long> getErrorLinkSids() {
        return _ErrorLinkSids;
    }

    public void setErrorLinkSids(java.util.ArrayList<Long> _v_) {
        if (_v_ == null)
            throw new IllegalArgumentException();
        _ErrorLinkSids = _v_;
    }

    @SuppressWarnings("deprecation")
    public Data() {
        _ErrorLinkSids = new java.util.ArrayList<>();
    }

    @SuppressWarnings("deprecation")
    public Data(java.util.ArrayList<Long> _ErrorLinkSids_) {
        if (_ErrorLinkSids_ == null)
            _ErrorLinkSids_ = new java.util.ArrayList<>();
        _ErrorLinkSids = _ErrorLinkSids_;
    }

    @Override
    public void reset() {
        _ErrorLinkSids.clear();
    }

    @Override
    public Zeze.Builtin.Provider.BSendResult toBean() {
        var _b_ = new Zeze.Builtin.Provider.BSendResult();
        _b_.assign(this);
        return _b_;
    }

    @Override
    public void assign(Zeze.Transaction.Bean _o_) {
        assign((BSendResult)_o_);
    }

    public void assign(BSendResult _o_) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(_o_._ErrorLinkSids);
    }

    public void assign(BSendResult.Data _o_) {
        _ErrorLinkSids.clear();
        _ErrorLinkSids.addAll(_o_._ErrorLinkSids);
    }

    @Override
    public BSendResult.Data copy() {
        var _c_ = new BSendResult.Data();
        _c_.assign(this);
        return _c_;
    }

    public static void swap(BSendResult.Data _a_, BSendResult.Data _b_) {
        var _s_ = _a_.copy();
        _a_.assign(_b_);
        _b_.assign(_s_);
    }

    @Override
    public long typeId() {
        return TYPEID;
    }

    @Override
    public BSendResult.Data clone() {
        return (BSendResult.Data)super.clone();
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
        _s_.append("Zeze.Builtin.Provider.BSendResult: {\n");
        _s_.append(_i1_).append("ErrorLinkSids=[");
        if (!_ErrorLinkSids.isEmpty()) {
            _s_.append('\n');
            for (var _v_ : _ErrorLinkSids) {
                _s_.append(_i2_).append("Item=").append(_v_).append(",\n");
            }
            _s_.append(_i1_);
        }
        _s_.append("]\n");
        _s_.append(Zeze.Util.Str.indent(_l_)).append('}');
    }

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int _s_) {
        _PRE_ALLOC_SIZE_ = _s_;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            var _x_ = _ErrorLinkSids;
            int _n_ = _x_.size();
            if (_n_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
                _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                for (int _j_ = 0, _c_ = _x_.size(); _j_ < _c_; _j_++) {
                    var _v_ = _x_.get(_j_);
                    _o_.WriteLong(_v_);
                    _n_--;
                }
                if (_n_ != 0)
                    throw new java.util.ConcurrentModificationException(String.valueOf(_n_));
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            var _x_ = _ErrorLinkSids;
            _x_.clear();
            if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
                for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    _x_.add(_o_.ReadLong(_t_));
            } else
                _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    @Override
    public boolean equals(Object _o_) {
        if (_o_ == this)
            return true;
        if (!(_o_ instanceof BSendResult.Data))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BSendResult.Data)_o_;
        if (!_ErrorLinkSids.equals(_b_._ErrorLinkSids))
            return false;
        return true;
    }
}
}
