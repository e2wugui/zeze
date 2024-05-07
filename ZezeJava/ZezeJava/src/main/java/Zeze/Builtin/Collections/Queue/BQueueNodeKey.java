// auto-generated @formatter:off
package Zeze.Builtin.Collections.Queue;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BQueueNodeKey implements Zeze.Transaction.BeanKey, Comparable<BQueueNodeKey> {
    private String _Name; // LinkedMap的Name
    private long _NodeId;

    // for decode only
    public BQueueNodeKey() {
        _Name = "";
    }

    public BQueueNodeKey(String _Name_, long _NodeId_) {
        if (_Name_ == null)
            throw new IllegalArgumentException();
        if (_Name_.length() > 256)
            throw new IllegalArgumentException();
        this._Name = _Name_;
        this._NodeId = _NodeId_;
    }

    public String getName() {
        return _Name;
    }

    public long getNodeId() {
        return _NodeId;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Collections.Queue.BQueueNodeKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("Name=").append(_Name).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("NodeId=").append(_NodeId).append(System.lineSeparator());
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
            String _x_ = _Name;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _NodeId;
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
            _Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _NodeId = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BQueueNodeKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BQueueNodeKey)_o_;
        if (!_Name.equals(_b_._Name))
            return false;
        if (_NodeId != _b_._NodeId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _Name.hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(_NodeId);
        return _h_;
    }

    @Override
    public int compareTo(BQueueNodeKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _Name.compareTo(_o_._Name);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_NodeId, _o_._NodeId);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getNodeId() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _Name = rs.getString(_parents_name_ + "Name");
        if (_Name == null)
            _Name = "";
        _NodeId = rs.getLong(_parents_name_ + "NodeId");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "Name", _Name);
        st.appendLong(_parents_name_ + "NodeId", _NodeId);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "Name", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "NodeId", "long", "", ""));
        return vars;
    }
}
