package Game.Fight;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class BFighterId implements Serializable, java.lang.Comparable {
	public static final int TypeRole = 1;
	public static final int TypeMonster = 2;
	public static final int TypePet = 2; // ...

	private int _Type; // 战斗对象类型
	private long _InstanceId; // 战斗对象实例id，根据type含义不一样。

	// for decode only
	public BFighterId() {
	}

	public BFighterId(int _Type_, long _InstanceId_) {
		this._Type = _Type_;
		this._InstanceId = _InstanceId_;
	}

	public int getType() {
		return _Type;
	}
	public long getInstanceId() {
		return _InstanceId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Fight.BFighterId: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Type").Append("=").Append(getType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("InstanceId").Append("=").Append(getInstanceId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(2); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(_Type);
		_os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(_InstanceId);
	}

	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					_Type = _os_.ReadInt();
					break;
				case ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT:
					_InstanceId = _os_.ReadLong();
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
		boolean tempVar = _obj1_ instanceof BFighterId;
		BFighterId _obj_ = tempVar ? (BFighterId)_obj1_ : null;
		if (tempVar) {
			if (_Type != _obj_._Type) {
				return false;
			}
			if (_InstanceId != _obj_._InstanceId) {
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
		_h_ = _h_ * _prime_ + (new Integer(_Type)).hashCode();
		_h_ = _h_ * _prime_ + (new Long(_InstanceId)).hashCode();
		return _h_;
	}

	public int compareTo(Object _o1_) {
		if (_o1_ == this) {
			return 0;
		}
		boolean tempVar = _o1_ instanceof BFighterId;
		BFighterId _o_ = tempVar ? (BFighterId)_o1_ : null;
		if (tempVar) {
			int _c_;
			_c_ = (new Integer(_Type)).compareTo(_o_._Type);
			if (0 != _c_) {
				return _c_;
			}
			_c_ = (new Long(_InstanceId)).compareTo(_o_._InstanceId);
			if (0 != _c_) {
				return _c_;
			}
			return _c_;
		}
		throw new RuntimeException("CompareTo: another object is not Game.Fight.BFighterId");
	}

	public boolean NegativeCheck() {
		if (getType() < 0) {
			return true;
		}
		if (getInstanceId() < 0) {
			return true;
		}
		return false;
	}

}