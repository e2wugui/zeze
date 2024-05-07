// auto-generated @formatter:off
package Zeze.Builtin.Threading;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BLockName implements Zeze.Transaction.BeanKey, Comparable<BLockName> {
    private final Zeze.Builtin.Threading.BGlobalThreadId _GlobalThreadId;
    private String _Name;

    // for decode only
    public BLockName() {
        _GlobalThreadId = new Zeze.Builtin.Threading.BGlobalThreadId();
        _Name = "";
    }

    public BLockName(Zeze.Builtin.Threading.BGlobalThreadId _GlobalThreadId_, String _Name_) {
        if (_GlobalThreadId_ == null)
            throw new IllegalArgumentException();
        this._GlobalThreadId = _GlobalThreadId_;
        if (_Name_ == null)
            throw new IllegalArgumentException();
        if (_Name_.length() > 256)
            throw new IllegalArgumentException();
        this._Name = _Name_;
    }

    public Zeze.Builtin.Threading.BGlobalThreadId getGlobalThreadId() {
        return _GlobalThreadId;
    }

    public String getName() {
        return _Name;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Threading.BLockName: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("GlobalThreadId=").append(System.lineSeparator());
        _GlobalThreadId.buildString(sb, level + 4);
        sb.append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(_Name).append(System.lineSeparator());
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
            int _a_ = _o_.WriteIndex;
            int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
            int _b_ = _o_.WriteIndex;
            _GlobalThreadId.encode(_o_);
            if (_b_ + 1 == _o_.WriteIndex)
                _o_.WriteIndex = _a_;
            else
                _i_ = _j_;
        }
        {
            String _x_ = _Name;
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
            _o_.ReadBean(getGlobalThreadId(), _t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _Name = _o_.ReadString(_t_);
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
        if (!(_o_ instanceof BLockName))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLockName)_o_;
        if (!_GlobalThreadId.equals(_b_._GlobalThreadId))
            return false;
        if (!_Name.equals(_b_._Name))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _GlobalThreadId.hashCode();
        _h_ = _h_ * _p_ + _Name.hashCode();
        return _h_;
    }

    @Override
    public int compareTo(BLockName _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _GlobalThreadId.compareTo(_o_._GlobalThreadId);
            if (_c_ != 0)
                return _c_;
            _c_ = _Name.compareTo(_o_._Name);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getGlobalThreadId().negativeCheck())
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        parents.add("GlobalThreadId");
        _GlobalThreadId.decodeResultSet(parents, rs);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _Name = rs.getString(_parents_name_ + "Name");
        if (_Name == null)
            _Name = "";
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        parents.add("GlobalThreadId");
        _GlobalThreadId.encodeSQLStatement(parents, st);
        parents.remove(parents.size() - 1);
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Name", _Name);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "GlobalThreadId", "Zeze.Builtin.Threading.BGlobalThreadId", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "Name", "string", "", ""));
        return vars;
    }
}
