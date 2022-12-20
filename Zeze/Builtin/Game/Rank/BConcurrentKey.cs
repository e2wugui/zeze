// auto-generated
using System;
using Zeze.Serialize;

namespace Zeze.Builtin.Game.Rank
{
    public sealed class BConcurrentKey : Serializable, System.IComparable
    {
        public const int TimeTypeTotal = 0; // 所有时间
        public const int TimeTypeDay = 1; // 每天：Year为当前时间的年份，Offset为天
        public const int TimeTypeWeek = 2; // 每周：Year为当前时间的年份，Offset为周
        public const int TimeTypeSeason = 3; // 每季：Year为当前时间的年份，Offset为季
        public const int TimeTypeYear = 4; // 每年：Year为当前时间的年份，Offset为0
        public const int TimeTypeCustomize = 5; // 自定义：此时Offset是自定义Id，Year为0

        int _RankType;
        int _ConcurrentId; // = hash % ConcurrentLevel
        int _TimeType;
        int _Year;
        long _Offset; // 根据TimeType，含义不同

        // for decode only
        public BConcurrentKey()
        {
        }

        public BConcurrentKey(int _RankType_, int _ConcurrentId_, int _TimeType_, int _Year_, long _Offset_)
        {
            this._RankType = _RankType_;
            this._ConcurrentId = _ConcurrentId_;
            this._TimeType = _TimeType_;
            this._Year = _Year_;
            this._Offset = _Offset_;
        }

        public int RankType => _RankType;
        public int ConcurrentId => _ConcurrentId;
        public int TimeType => _TimeType;
        public int Year => _Year;
        public long Offset => _Offset;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Game.Rank.BConcurrentKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("RankType").Append('=').Append(RankType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ConcurrentId").Append('=').Append(ConcurrentId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TimeType").Append('=').Append(TimeType).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Year").Append('=').Append(Year).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Offset").Append('=').Append(Offset).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = _RankType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = _ConcurrentId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = _TimeType;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = _Year;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = _Offset;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            _o_.WriteByte(0);
        }

        public void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                _RankType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _ConcurrentId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                _TimeType = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                _Year = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                _Offset = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool Equals(object _o_)
        {
            if (_o_ == this) return true;
            if (_o_ is BConcurrentKey _b_)
            {
                if (_RankType != _b_._RankType) return false;
                if (_ConcurrentId != _b_._ConcurrentId) return false;
                if (_TimeType != _b_._TimeType) return false;
                if (_Year != _b_._Year) return false;
                if (_Offset != _b_._Offset) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _p_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _p_ + _RankType.GetHashCode();
            _h_ = _h_ * _p_ + _ConcurrentId.GetHashCode();
            _h_ = _h_ * _p_ + _TimeType.GetHashCode();
            _h_ = _h_ * _p_ + _Year.GetHashCode();
            _h_ = _h_ * _p_ + _Offset.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is BConcurrentKey _o_)
            {
                int _c_;
                _c_ = _RankType.CompareTo(_o_._RankType);
                if (_c_ != 0) return _c_;
                _c_ = _ConcurrentId.CompareTo(_o_._ConcurrentId);
                if (_c_ != 0) return _c_;
                _c_ = _TimeType.CompareTo(_o_._TimeType);
                if (_c_ != 0) return _c_;
                _c_ = _Year.CompareTo(_o_._Year);
                if (_c_ != 0) return _c_;
                _c_ = _Offset.CompareTo(_o_._Offset);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zeze.Builtin.Game.Rank.BConcurrentKey");
        }

        public bool NegativeCheck()
        {
            if (RankType < 0) return true;
            if (ConcurrentId < 0) return true;
            if (TimeType < 0) return true;
            if (Year < 0) return true;
            if (Offset < 0) return true;
            return false;
        }
    }
}
