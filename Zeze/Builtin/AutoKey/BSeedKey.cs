// auto-generated
using System;
using Zeze.Serialize;

// ReSharper disable ArrangeThisQualifier JoinDeclarationAndInitializer NonReadonlyMemberInGetHashCode
// ReSharper disable PossibleUnintendedReferenceComparison RedundantAssignment RedundantNameQualifier
// ReSharper disable StringCompareToIsCultureSpecific UselessBinaryOperation
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.AutoKey
{
    public sealed class BSeedKey : Serializable, IComparable
    {
        int _ServerId;
        string _KeyName;

        // for decode only
        public BSeedKey()
        {
            _KeyName = "";
        }

        public BSeedKey(int _ServerId_, string _KeyName_)
        {
            this._ServerId = _ServerId_;
            this._KeyName = _KeyName_;
        }

        public int ServerId => _ServerId;
        public string KeyName => _KeyName;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.AutoKey.BSeedKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ServerId").Append('=').Append(ServerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("KeyName").Append('=').Append(KeyName).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = _ServerId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = _KeyName;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                _ServerId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _KeyName = _o_.ReadString(_t_);
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
            if (_o_ is BSeedKey _b_)
            {
                if (_ServerId != _b_._ServerId) return false;
                if (!_KeyName.Equals(_b_._KeyName)) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _p_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _p_ + _ServerId.GetHashCode();
            _h_ = _h_ * _p_ + _KeyName.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is BSeedKey _o_)
            {
                int _c_;
                _c_ = _ServerId.CompareTo(_o_._ServerId);
                if (_c_ != 0) return _c_;
                _c_ = _KeyName.CompareTo(_o_._KeyName);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zeze.Builtin.AutoKey.BSeedKey");
        }

        public bool NegativeCheck()
        {
            if (ServerId < 0) return true;
            return false;
        }
    }
}
