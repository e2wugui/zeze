// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

@SuppressWarnings({"MethodMayBeStatic", "NullableProblems", "PatternVariableCanBeUsed", "RedundantIfStatement", "RedundantSuppression", "SuspiciousNameCombination", "UnusedAssignment"})
public final class BConcurrentKey implements Zeze.Transaction.BeanKey, Comparable<BConcurrentKey> {
    public static final int TimeTypeTotal = 0; // 所有时间
    public static final int TimeTypeDay = 1; // 每天：Year为当前时间的年份，Offset为天
    public static final int TimeTypeWeek = 2; // 每周：Year为当前时间的年份，Offset为周
    public static final int TimeTypeSeason = 3; // 每季：Year为当前时间的年份，Offset为季
    public static final int TimeTypeYear = 4; // 每年：Year为当前时间的年份，Offset为0
    public static final int TimeTypeCustomize = 5; // 自定义：此时Offset是自定义Id，Year为0

    private int _RankType;
    private int _ConcurrentId; // = hash % ConcurrentLevel
    private int _TimeType;
    private int _Year;
    private long _Offset; // 根据TimeType，含义不同

    // for decode only
    public BConcurrentKey() {
    }

    public BConcurrentKey(int _RankType_, int _ConcurrentId_, int _TimeType_, int _Year_, long _Offset_) {
        this._RankType = _RankType_;
        this._ConcurrentId = _ConcurrentId_;
        this._TimeType = _TimeType_;
        this._Year = _Year_;
        this._Offset = _Offset_;
    }

    public int getRankType() {
        return _RankType;
    }

    public int getConcurrentId() {
        return _ConcurrentId;
    }

    public int getTimeType() {
        return _TimeType;
    }

    public int getYear() {
        return _Year;
    }

    public long getOffset() {
        return _Offset;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        buildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void buildString(StringBuilder sb, int level) {
        sb.append(Zeze.Util.Str.indent(level)).append("Zeze.Builtin.Game.Rank.BConcurrentKey: {").append(System.lineSeparator());
        level += 4;
        sb.append(Zeze.Util.Str.indent(level)).append("RankType=").append(_RankType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("ConcurrentId=").append(_ConcurrentId).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("TimeType=").append(_TimeType).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Year=").append(_Year).append(',').append(System.lineSeparator());
        sb.append(Zeze.Util.Str.indent(level)).append("Offset=").append(_Offset).append(System.lineSeparator());
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
            int _x_ = _RankType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _ConcurrentId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _TimeType;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            int _x_ = _Year;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                _o_.WriteInt(_x_);
            }
        }
        {
            long _x_ = _Offset;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
            _RankType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            _ConcurrentId = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 3) {
            _TimeType = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 4) {
            _Year = _o_.ReadInt(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 5) {
            _Offset = _o_.ReadLong(_t_);
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
        if (!(_o_ instanceof BConcurrentKey))
            return false;
        //noinspection PatternVariableCanBeUsed
        var _b_ = (BConcurrentKey)_o_;
        if (_RankType != _b_._RankType)
            return false;
        if (_ConcurrentId != _b_._ConcurrentId)
            return false;
        if (_TimeType != _b_._TimeType)
            return false;
        if (_Year != _b_._Year)
            return false;
        if (_Offset != _b_._Offset)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int _p_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _p_ + Integer.hashCode(_RankType);
        _h_ = _h_ * _p_ + Integer.hashCode(_ConcurrentId);
        _h_ = _h_ * _p_ + Integer.hashCode(_TimeType);
        _h_ = _h_ * _p_ + Integer.hashCode(_Year);
        _h_ = _h_ * _p_ + Long.hashCode(_Offset);
        return _h_;
    }

    @Override
    public int compareTo(BConcurrentKey _o_) {
        if (_o_ == this)
            return 0;
        if (_o_ != null) {
            int _c_;
            _c_ = Integer.compare(_RankType, _o_._RankType);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_ConcurrentId, _o_._ConcurrentId);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_TimeType, _o_._TimeType);
            if (_c_ != 0)
                return _c_;
            _c_ = Integer.compare(_Year, _o_._Year);
            if (_c_ != 0)
                return _c_;
            _c_ = Long.compare(_Offset, _o_._Offset);
            if (_c_ != 0)
                return _c_;
            return _c_;
        }
        throw new NullPointerException("compareTo: another object is null");
    }

    public boolean negativeCheck() {
        if (getRankType() < 0)
            return true;
        if (getConcurrentId() < 0)
            return true;
        if (getTimeType() < 0)
            return true;
        if (getYear() < 0)
            return true;
        if (getOffset() < 0)
            return true;
        return false;
    }

    @Override
    public void decodeResultSet(java.util.ArrayList<String> parents, java.sql.ResultSet rs) throws java.sql.SQLException {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        _RankType = rs.getInt(_parents_name_ + "RankType");
        _ConcurrentId = rs.getInt(_parents_name_ + "ConcurrentId");
        _TimeType = rs.getInt(_parents_name_ + "TimeType");
        _Year = rs.getInt(_parents_name_ + "Year");
        _Offset = rs.getLong(_parents_name_ + "Offset");
    }

    @Override
    public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {
        var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
        st.appendInt(_parents_name_ + "RankType", _RankType);
        st.appendInt(_parents_name_ + "ConcurrentId", _ConcurrentId);
        st.appendInt(_parents_name_ + "TimeType", _TimeType);
        st.appendInt(_parents_name_ + "Year", _Year);
        st.appendLong(_parents_name_ + "Offset", _Offset);
    }

    @Override
    public java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables() {
        var vars = new java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data>();
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(1, "RankType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(2, "ConcurrentId", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(3, "TimeType", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(4, "Year", "int", "", ""));
        vars.add(new Zeze.Builtin.HotDistribute.BVariable.Data(5, "Offset", "long", "", ""));
        return vars;
    }
}
