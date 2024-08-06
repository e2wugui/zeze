// auto-generated @formatter:off
package Zeze.Builtin.Collections.DepartmentTree;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BDepartmentKey implements Zeze.Transaction.BeanKey, Comparable<BDepartmentKey> {
    private String _Owner;
    private long _DepartmentId;

    // for decode only
    public BDepartmentKey() {
        _Owner = "";
    }

    public BDepartmentKey(String _Owner_, long _DepartmentId_) {
        if (_Owner_ == null)
            throw new IllegalArgumentException();
        if (_Owner_.length() > 256)
            throw new IllegalArgumentException();
        this._Owner = _Owner_;
        this._DepartmentId = _DepartmentId_;
    }

    public String getOwner() {
        return _Owner;
    }

    public long getDepartmentId() {
        return _DepartmentId;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.Collections.DepartmentTree.BDepartmentKey: {\n");
        _s_.append(_i1_).append("Owner=").append(_Owner).append(",\n");
        _s_.append(_i1_).append("DepartmentId=").append(_DepartmentId).append('\n');
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

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = _Owner;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _DepartmentId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _Owner = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _DepartmentId = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BDepartmentKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BDepartmentKey)_o_;
        if (!_Owner.equals(_b_._Owner))
            return false;
        if (_DepartmentId != _b_._DepartmentId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Owner.hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(_DepartmentId);
        return _h_;
    }

    @Override
    public int compareTo(BDepartmentKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _Owner.compareTo(_o_._Owner);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_DepartmentId, _o_._DepartmentId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getDepartmentId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _Owner = _r_.getString(_pn_ + "Owner");
        if (_Owner == null)
            _Owner = "";
        _DepartmentId = _r_.getLong(_pn_ + "DepartmentId");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendString(_pn_ + "Owner", _Owner);
        _s_.appendLong(_pn_ + "DepartmentId", _DepartmentId);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Owner", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "DepartmentId", "long", "", ""));
        return vars;
    }
}
