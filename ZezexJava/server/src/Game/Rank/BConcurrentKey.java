package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class BConcurrentKey implements Serializable, java.lang.Comparable {
	public static final int RankTypeGold = 1;
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
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Rank.BConcurrentKey: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("RankType").Append("=").Append(getRankType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ConcurrentId").Append("=").Append(getConcurrentId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("TimeType").Append("=").Append(getTimeType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Year").Append("=").Append(getYear()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Offset").Append("=").Append(getOffset()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(5); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(_RankType);
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(_ConcurrentId);
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(_TimeType);
		_os_.WriteInt(ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(_Year);
		_os_.WriteInt(ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(_Offset);
	}

	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					_RankType = _os_.ReadInt();
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					_ConcurrentId = _os_.ReadInt();
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					_TimeType = _os_.ReadInt();
					break;
				case ByteBuffer.INT | 4 << ByteBuffer.TAG_SHIFT:
					_Year = _os_.ReadInt();
					break;
				case ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT:
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
		if (_obj1_ == this) {
			return true;
		}
		boolean tempVar = _obj1_ instanceof BConcurrentKey;
		BConcurrentKey _obj_ = tempVar ? (BConcurrentKey)_obj1_ : null;
		if (tempVar) {
			if (_RankType != _obj_._RankType) {
				return false;
			}
			if (_ConcurrentId != _obj_._ConcurrentId) {
				return false;
			}
			if (_TimeType != _obj_._TimeType) {
				return false;
			}
			if (_Year != _obj_._Year) {
				return false;
			}
			if (_Offset != _obj_._Offset) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int _prime_ = 31;
		int _h_ = 0;
		_h_ = _h_ * _prime_ + (new Integer(_RankType)).hashCode();
		_h_ = _h_ * _prime_ + (new Integer(_ConcurrentId)).hashCode();
		_h_ = _h_ * _prime_ + (new Integer(_TimeType)).hashCode();
		_h_ = _h_ * _prime_ + (new Integer(_Year)).hashCode();
		_h_ = _h_ * _prime_ + (new Long(_Offset)).hashCode();
		return _h_;
	}

	public int compareTo(Object _o1_) {
		if (_o1_ == this) {
			return 0;
		}
		boolean tempVar = _o1_ instanceof BConcurrentKey;
		BConcurrentKey _o_ = tempVar ? (BConcurrentKey)_o1_ : null;
		if (tempVar) {
			int _c_;
			_c_ = (new Integer(_RankType)).compareTo(_o_._RankType);
			if (0 != _c_) {
				return _c_;
			}
			_c_ = (new Integer(_ConcurrentId)).compareTo(_o_._ConcurrentId);
			if (0 != _c_) {
				return _c_;
			}
			_c_ = (new Integer(_TimeType)).compareTo(_o_._TimeType);
			if (0 != _c_) {
				return _c_;
			}
			_c_ = (new Integer(_Year)).compareTo(_o_._Year);
			if (0 != _c_) {
				return _c_;
			}
			_c_ = (new Long(_Offset)).compareTo(_o_._Offset);
			if (0 != _c_) {
				return _c_;
			}
			return _c_;
		}
		throw new RuntimeException("CompareTo: another object is not Game.Rank.BConcurrentKey");
	}

	public boolean NegativeCheck() {
		if (getRankType() < 0) {
			return true;
		}
		if (getConcurrentId() < 0) {
			return true;
		}
		if (getTimeType() < 0) {
			return true;
		}
		if (getYear() < 0) {
			return true;
		}
		if (getOffset() < 0) {
			return true;
		}
		return false;
	}

}