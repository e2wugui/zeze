// auto-generated
package Game.Fight;

import Zeze.Serialize.*;

public final class BFighterId implements Serializable, Comparable<BFighterId> {
    public final static int TypeRole = 1;
    public final static int TypeMonster = 2;
    public final static int TypePet = 2; // ...

    private int _Type; // 战斗对象类型
    private long _InstanceId; // 战斗对象实例id，根据type含义不一样。

    // for decode only
    public BFighterId() {
    }

    public BFighterId(int _Type_, long _InstanceId_) {
        this._Type = _Type_;
        this._InstanceId = _InstanceId_;
    }

    public int getType(){
        return _Type;
    }

    public long getInstanceId(){
        return _InstanceId;
    }


    @Override
    public String toString() {
        var sb = new StringBuilder();
        BuildString(sb, 0);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public void BuildString(StringBuilder sb, int level) {
        sb.append(" ".repeat(level * 4)).append("Game.Fight.BFighterId: {").append(System.lineSeparator());
        level++;
        sb.append(" ".repeat(level * 4)).append("Type").append("=").append(getType()).append(",").append(System.lineSeparator());
        sb.append(" ".repeat(level * 4)).append("InstanceId").append("=").append(getInstanceId()).append("").append(System.lineSeparator());
        sb.append("}");
    }

    @Override
    public void Encode(ByteBuffer _os_) {
        _os_.WriteInt(2); // Variables.Count
        _os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
        _os_.WriteInt(getType());
        _os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
        _os_.WriteLong(getInstanceId());
    }

    @Override
    public void Decode(ByteBuffer _os_) {
        for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
            int _tagid_ = _os_.ReadInt();
            switch (_tagid_) {
                case (ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT): 
                    _Type = _os_.ReadInt();
                    break;
                case (ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT): 
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
        if (_obj1_ == this) return true;
        if (_obj1_ instanceof BFighterId) {
            var _obj_ = (BFighterId)_obj1_;
            if (Integer.compare(getType(), _obj_.getType()) != 0) return false;
            if (Long.compare(getInstanceId(), _obj_.getInstanceId()) != 0) return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int _prime_ = 31;
        int _h_ = 0;
        _h_ = _h_ * _prime_ + Integer.hashCode(_Type);
        _h_ = _h_ * _prime_ + Long.hashCode(_InstanceId);
        return _h_;
    }

    @Override
    public int compareTo(BFighterId _o1_) {
        if (_o1_ == this) return 0;
        if (_o1_ instanceof BFighterId) {
            var _o_ = (BFighterId)_o1_;
            int _c_;
            _c_ = Integer.compare(_Type, _o_._Type);
            if (0 != _c_) return _c_;
            _c_ = Long.compare(_InstanceId, _o_._InstanceId);
            if (0 != _c_) return _c_;
            return _c_;
        }
        throw new RuntimeException("CompareTo: another object is not Game.Fight.BFighterId");
    }

    public boolean NegativeCheck() {
        if (getType() < 0) return true;
        if (getInstanceId() < 0) return true;
        return false;
    }

}
