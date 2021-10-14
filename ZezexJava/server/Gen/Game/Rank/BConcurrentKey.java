// auto-generated
package Game.Rank;

import Zeze.Serialize.*;

public final class BConcurrentKey implements Serializable, Comparable<BConcurrentKey> {
    public final static int RankTypeGold = 1;
    public final static int TimeTypeTotal = 0; // 所有时间
    public final static int TimeTypeDay = 1; // 每天：Year为当前时间的年份，Offset为天
    public final static int TimeTypeWeek = 2; // 每周：Year为当前时间的年份，Offset为周
    public final static int TimeTypeSeason = 3; // 每季：Year为当前时间的年份，Offset为季
    public final static int TimeTypeYear = 4; // 每年：Year为当前时间的年份，Offset为0
    public final static int TimeTypeCustomize = 5; // 自定义：此时Offset是自定义Id，Year为0

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

    public int getRankType(){
        return _RankType;
    }

    public int getConcurrentId(){
        return _ConcurrentId;
    }

    public int getTimeType(){
        return _TimeType;
    }

    public int getYear(){
        return _Year;
    }

    public long getOffset(){
        return _Offset;
    }


    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void BuildString(StringBuilder sb, int level) {
        sb.append(" ".repeat(level * 4)).append("Game.Rank.BConcurrentKey: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("RankType").append("=").append(getRankType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("ConcurrentId").append("=").append(getConcurrentId()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("TimeType").append("=").append(getTimeType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Year").append("=").append(getYear()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("Offset").append("=").append(getOffset()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(5); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getRankType());
        _os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getConcurrentId());
        _os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getTimeType());
        _os_.WriteInt(ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getYear());
        _os_.WriteInt(ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getOffset());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    _RankType = _os_.ReadInt();
                    break;
                case (ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT): 
                    _ConcurrentId = _os_.ReadInt();
                    break;
                case (ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT): 
                    _TimeType = _os_.ReadInt();
                    break;
                case (ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT): 
                    _Year = _os_.ReadInt();
                    break;
                case (ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT): 
                    _Offset = _os_.ReadLong();
                    break;
                default:
                    ByteBuffer.SkipUnknownField(_tagid_, _os_);
                    break;
            }
        }
    }

    @Override
    public boolean equals(Object _obj1_) {
        if (_obj1_ == this) return true;
        if (_obj1_ instanceof BConcurrentKey) {
            var _obj_ = (BConcurrentKey)_obj1_;
            if (Integer.compare(getRankType(), _obj_.getRankType()) != 0) return false;
            if (Integer.compare(getConcurrentId(), _obj_.getConcurrentId()) != 0) return false;
            if (Integer.compare(getTimeType(), _obj_.getTimeType()) != 0) return false;
            if (Integer.compare(getYear(), _obj_.getYear()) != 0) return false;
            if (Long.compare(getOffset(), _obj_.getOffset()) != 0) return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + Integer.hashCode(_RankType);
        _h_ = _h_ * _prime_ + Integer.hashCode(_ConcurrentId);
        _h_ = _h_ * _prime_ + Integer.hashCode(_TimeType);
        _h_ = _h_ * _prime_ + Integer.hashCode(_Year);
        _h_ = _h_ * _prime_ + Long.hashCode(_Offset);
        return _h_;
    }

    @Override
    public int compareTo(BConcurrentKey _o1_) {
        if (_o1_ == this) return 0;
        if (_o1_ instanceof BConcurrentKey) {
            var _o_ = (BConcurrentKey)_o1_;
            int _c_;
            _c_ = Integer.compare(_RankType, _o_._RankType);
            if (0 != _c_) return _c_;
            _c_ = Integer.compare(_ConcurrentId, _o_._ConcurrentId);
            if (0 != _c_) return _c_;
            _c_ = Integer.compare(_TimeType, _o_._TimeType);
            if (0 != _c_) return _c_;
            _c_ = Integer.compare(_Year, _o_._Year);
            if (0 != _c_) return _c_;
            _c_ = Long.compare(_Offset, _o_._Offset);
            if (0 != _c_) return _c_;
            return _c_;
        }
        throw new RuntimeException("CompareTo: another object is not Game.Rank.BConcurrentKey");
    }

    public boolean NegativeCheck() {
        if (getRankType() < 0) return true;
        if (getConcurrentId() < 0) return true;
        if (getTimeType() < 0) return true;
        if (getYear() < 0) return true;
        if (getOffset() < 0) return true;
        return false;
    }

}
