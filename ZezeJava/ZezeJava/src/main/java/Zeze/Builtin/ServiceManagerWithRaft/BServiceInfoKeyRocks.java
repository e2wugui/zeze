// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BServiceInfoKeyRocks implements Zeze.Transaction.BeanKey, Comparable<BServiceInfoKeyRocks> {
    private String _ServiceName;
    private String _ServiceIdentity;

    // for decode only
    public BServiceInfoKeyRocks() {
        _ServiceName = "";
        _ServiceIdentity = "";
    }

    public BServiceInfoKeyRocks(String _ServiceName_, String _ServiceIdentity_) {
        if (_ServiceName_ == null)
            throw new IllegalArgumentException();
        if (_ServiceName_.length() > 256)
            throw new IllegalArgumentException();
        this._ServiceName = _ServiceName_;
        if (_ServiceIdentity_ == null)
            throw new IllegalArgumentException();
        if (_ServiceIdentity_.length() > 256)
            throw new IllegalArgumentException();
        this._ServiceIdentity = _ServiceIdentity_;
    }

    public String getServiceName() {
        return _ServiceName;
    }

    public String getServiceIdentity() {
        return _ServiceIdentity;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.ServiceManagerWithRaft.BServiceInfoKeyRocks: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceName=").append(_ServiceName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ServiceIdentity=").append(_ServiceIdentity).append(System.lineSeparator());
        level -= 4;
        sb.append(Zeze.Util.Str.indent(level)).append('}');
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int preAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void preAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }

    @Override
    public void encode(ByteBuffer _o_) {
        int _i_ = 0;
        {
            String _x_ = _ServiceName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            String _x_ = _ServiceIdentity;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _ServiceName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ServiceIdentity = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BServiceInfoKeyRocks))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BServiceInfoKeyRocks)_o_;
        if (!_ServiceName.equals(_b_._ServiceName))
            return false;
        if (!_ServiceIdentity.equals(_b_._ServiceIdentity))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _ServiceName.hashCode();
        _h_ = _h_ * _p_ + _ServiceIdentity.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BServiceInfoKeyRocks _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _ServiceName.compareTo(_o_._ServiceName);
            if (_c_ != 0)
                return _c_;
            _c_ = _ServiceIdentity.compareTo(_o_._ServiceIdentity);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _ServiceName = rs.getString(_parents_name_ + "ServiceName");
        if (_ServiceName == null)
            _ServiceName = "";
        _ServiceIdentity = rs.getString(_parents_name_ + "ServiceIdentity");
        if (_ServiceIdentity == null)
            _ServiceIdentity = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "ServiceName", _ServiceName);
        st.appendString(_parents_name_ + "ServiceIdentity", _ServiceIdentity);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "ServiceName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ServiceIdentity", "string", "", ""));
        return vars;
    }
}
