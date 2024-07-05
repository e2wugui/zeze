// auto-generated
using System;
using Zeze.Serialize;

// ReSharper disable ArrangeThisQualifier JoinDeclarationAndInitializer NonReadonlyMemberInGetHashCode
// ReSharper disable PossibleUnintendedReferenceComparison RedundantAssignment RedundantNameQualifier
// ReSharper disable StringCompareToIsCultureSpecific UselessBinaryOperation
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.LinkedMap
{
    public sealed class BLinkedMapKey : Serializable, IComparable
    {
        string _Name; // LinkedMap的Name
        string _ValueId; // LinkedMap的Key转成字符串类型

        // for decode only
        public BLinkedMapKey()
        {
            _Name = "";
            _ValueId = "";
        }

        public BLinkedMapKey(string _Name_, string _ValueId_)
        {
            this._Name = _Name_;
            this._ValueId = _ValueId_;
        }

        public string Name => _Name;
        public string ValueId => _ValueId;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Name").Append('=').Append(Name).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ValueId").Append('=').Append(ValueId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = _Name;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = _ValueId;
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
                _Name = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _ValueId = _o_.ReadString(_t_);
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
            if (_o_ is BLinkedMapKey _b_)
            {
                if (!_Name.Equals(_b_._Name)) return false;
                if (!_ValueId.Equals(_b_._ValueId)) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _p_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _p_ + _Name.GetHashCode();
            _h_ = _h_ * _p_ + _ValueId.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is BLinkedMapKey _o_)
            {
                int _c_;
                _c_ = _Name.CompareTo(_o_._Name);
                if (_c_ != 0) return _c_;
                _c_ = _ValueId.CompareTo(_o_._ValueId);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zeze.Builtin.Collections.LinkedMap.BLinkedMapKey");
        }

        public bool NegativeCheck()
        {
            return false;
        }
    }
}
