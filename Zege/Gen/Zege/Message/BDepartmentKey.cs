// auto-generated
using System;
using Zeze.Serialize;

// 群（部门）消息历史
// ReSharper disable ArrangeThisQualifier JoinDeclarationAndInitializer NonReadonlyMemberInGetHashCode
// ReSharper disable PossibleUnintendedReferenceComparison RedundantAssignment RedundantNameQualifier
// ReSharper disable StringCompareToIsCultureSpecific UselessBinaryOperation
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public sealed class BDepartmentKey : Serializable, IComparable
    {
        string _Group;
        long _DepartmentId;

        // for decode only
        public BDepartmentKey()
        {
            _Group = "";
        }

        public BDepartmentKey(string _Group_, long _DepartmentId_)
        {
            this._Group = _Group_;
            this._DepartmentId = _DepartmentId_;
        }

        public string Group => _Group;
        public long DepartmentId => _DepartmentId;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BDepartmentKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Group").Append('=').Append(Group).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("DepartmentId").Append('=').Append(DepartmentId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = _Group;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = _DepartmentId;
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
                _Group = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _DepartmentId = _o_.ReadLong(_t_);
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
            if (_o_ is BDepartmentKey _b_)
            {
                if (!_Group.Equals(_b_._Group)) return false;
                if (_DepartmentId != _b_._DepartmentId) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _p_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _p_ + _Group.GetHashCode();
            _h_ = _h_ * _p_ + _DepartmentId.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is BDepartmentKey _o_)
            {
                int _c_;
                _c_ = _Group.CompareTo(_o_._Group);
                if (_c_ != 0) return _c_;
                _c_ = _DepartmentId.CompareTo(_o_._DepartmentId);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zege.Message.BDepartmentKey");
        }

        public bool NegativeCheck()
        {
            if (DepartmentId < 0) return true;
            return false;
        }
    }
}
