// auto-generated
using System;
using Zeze.Serialize;

// ReSharper disable ArrangeThisQualifier JoinDeclarationAndInitializer NonReadonlyMemberInGetHashCode
// ReSharper disable PossibleUnintendedReferenceComparison RedundantAssignment RedundantNameQualifier
// ReSharper disable StringCompareToIsCultureSpecific UselessBinaryOperation
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class BDepartmentMessageKey : Serializable, IComparable
    {
        Zege.Message.BDepartmentKey _GroupDepartment; // 重用结构，节约内存
        long _MessageId;

        // for decode only
        public BDepartmentMessageKey()
        {
            _GroupDepartment = new Zege.Message.BDepartmentKey();
        }

        public BDepartmentMessageKey(Zege.Message.BDepartmentKey _GroupDepartment_, long _MessageId_)
        {
            this._GroupDepartment = _GroupDepartment_;
            this._MessageId = _MessageId_;
        }

        public Zege.Message.BDepartmentKey GroupDepartment => _GroupDepartment;
        public long MessageId => _MessageId;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BDepartmentMessageKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("GroupDepartment").Append('=').Append(Environment.NewLine);
            GroupDepartment.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MessageId").Append('=').Append(MessageId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                _GroupDepartment.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            {
                long _x_ = _MessageId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
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
                _o_.ReadBean(_GroupDepartment, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _MessageId = _o_.ReadLong(_t_);
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
            if (_o_ is BDepartmentMessageKey _b_)
            {
                if (!_GroupDepartment.Equals(_b_._GroupDepartment)) return false;
                if (_MessageId != _b_._MessageId) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _p_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _p_ + _GroupDepartment.GetHashCode();
            _h_ = _h_ * _p_ + _MessageId.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is BDepartmentMessageKey _o_)
            {
                int _c_;
                _c_ = _GroupDepartment.CompareTo(_o_._GroupDepartment);
                if (_c_ != 0) return _c_;
                _c_ = _MessageId.CompareTo(_o_._MessageId);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zege.Message.BDepartmentMessageKey");
        }

        public bool NegativeCheck()
        {
            if (GroupDepartment.NegativeCheck()) return true;
            if (MessageId < 0) return true;
            return false;
        }
    }
}
