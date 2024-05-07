// auto-generated @formatter:off
package Zeze.Builtin.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

// tables
@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BLink implements Zeze.Transaction.BeanKey, Comparable<BLink> {
    private String _LinkName;
    private long _LinkSid;
    private int _State;

    // for decode only
    public BLink() {
        _LinkName = "";
    }

    public BLink(String _LinkName_, long _LinkSid_, int _State_) {
        if (_LinkName_ == null)
            throw new IllegalArgumentException();
        if (_LinkName_.length() > 256)
            throw new IllegalArgumentException();
        this._LinkName = _LinkName_;
        this._LinkSid = _LinkSid_;
        this._State = _State_;
    }

    public String getLinkName() {
        return _LinkName;
    }

    public long getLinkSid() {
        return _LinkSid;
    }

    public int getState() {
        return _State;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Online.BLink: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("LinkName=").append(_LinkName).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("LinkSid=").append(_LinkSid).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("State=").append(_State).append(System.lineSeparator());
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
            String _x_ = _LinkName;
            if (!_x_.isEmpty()) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            long _x_ = _LinkSid;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        {
            int _x_ = _State;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    @Override
    public void decode(IByteBuffer _o_) {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            _LinkName = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _LinkSid = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _State = _o_.ReadInt(_t_);
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
        if (!(_o_ instanceof BLink))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BLink)_o_;
        if (!_LinkName.equals(_b_._LinkName))
            return false;
        if (_LinkSid != _b_._LinkSid)
            return false;
        if (_State != _b_._State)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + _LinkName.hashCode();
        _h_ = _h_ * _p_ + Long.hashCode(_LinkSid);
        _h_ = _h_ * _p_ + Integer.hashCode(_State);
        return _h_;
    }

    @Override
    public int compareTo(BLink _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = _LinkName.compareTo(_o_._LinkName);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_LinkSid, _o_._LinkSid);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_State, _o_._State);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getLinkSid() < 0)
            return true;
        if (getState() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _LinkName = rs.getString(_parents_name_ + "LinkName");
        if (_LinkName == null)
            _LinkName = "";
        _LinkSid = rs.getLong(_parents_name_ + "LinkSid");
        _State = rs.getInt(_parents_name_ + "State");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendString(_parents_name_ + "LinkName", _LinkName);
        st.appendLong(_parents_name_ + "LinkSid", _LinkSid);
        st.appendInt(_parents_name_ + "State", _State);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "LinkName", "string", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "LinkSid", "long", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "State", "int", "", ""));
        return vars;
    }
}
