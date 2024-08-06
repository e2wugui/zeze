// auto-generated @formatter:off
package Zeze.Builtin.HistoryModule;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BTableKey implements Zeze.Transaction.BeanKey, Comparable<BTableKey> {
    private int _TableId;
    private Zeze.Net.Binary _KeyEncoded;

    // for decode only
    public BTableKey() {
        _KeyEncoded = Zeze.Net.Binary.Empty;
    }

    public BTableKey(int _TableId_, Zeze.Net.Binary _KeyEncoded_) {
        this._TableId = _TableId_;
        if (_KeyEncoded_ == null)
            throw new IllegalArgumentException();
        this._KeyEncoded = _KeyEncoded_;
    }

    public int getTableId() {
        return _TableId;
    }

    public Zeze.Net.Binary getKeyEncoded() {
        return _KeyEncoded;
    }

    @Override
    public String toString() {
        var _s_ = new StringBuilder();
        buildString(_s_, 0);
        return _s_.toString();
    }

    public void buildString(StringBuilder _s_, int _l_) {
        var _i1_ = Zeze.Util.Str.indent(_l_ + 4);
        _s_.append("Zeze.Builtin.HistoryModule.BTableKey: {\n");
        _s_.append(_i1_).append("TableId=").append(_TableId).append(",\n");
        _s_.append(_i1_).append("KeyEncoded=").append(_KeyEncoded).append('\n');
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
            int _x_ = _TableId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            var _x_ = _KeyEncoded;
            if (_x_.size() != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteBinary(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _TableId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _KeyEncoded = _o_.ReadBinary(_t_);
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
        if (!(_o_ instanceof BTableKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BTableKey)_o_;
        if (_TableId != _b_._TableId)
            return false;
        if (!_KeyEncoded.equals(_b_._KeyEncoded))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_TableId);
        _h_ = _h_ * _p_ + _KeyEncoded.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BTableKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_TableId, _o_._TableId);
            if (_c_ != 0)
                return _c_;
            _c_ = _KeyEncoded.compareTo(_o_._KeyEncoded);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getTableId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> _p_, java.sql.ResultSet _r_) throws java.sql.SQLException {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _TableId = _r_.getInt(_pn_ + "TableId");
        _KeyEncoded = new Zeze.Net.Binary(_r_.getBytes(_pn_ + "KeyEncoded"));
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_) {
        var _pn_ = Zeze.Transaction.Bean.parentsToName(_p_);
        _s_.appendInt(_pn_ + "TableId", _TableId);
        _s_.appendBinary(_pn_ + "KeyEncoded", _KeyEncoded);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "TableId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "KeyEncoded", "binary", "", ""));
        return vars;
    }
}
