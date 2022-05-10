// auto-generated
using System;
using Zeze.Serialize;

namespace Zeze.Builtin.GlobalCacheManagerWithRaft
{
    public sealed class GlobalTableKey : Serializable, System.IComparable
    {
        int _Id;
        Zeze.Net.Binary _Key;

        // for decode only
        public GlobalTableKey()
        {
            _Key = Zeze.Net.Binary.Empty;
        }

        public GlobalTableKey(int _Id_, Zeze.Net.Binary _Key_)
        {
            this._Id = _Id_;
            this._Key = _Key_;
        }

        public int Id => _Id;
        public Zeze.Net.Binary Key => _Key;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Id").Append('=').Append(Id).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = _Id;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                var _x_ = _Key;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
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
                _Id = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _Key = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool Equals(object _obj1_)
        {
            if (_obj1_ == this) return true;
            if (_obj1_ is GlobalTableKey _obj_)
            {
                if (_Id != _obj_._Id) return false;
                if (!_Key.Equals(_obj_._Key)) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + _Id.GetHashCode();
            _h_ = _h_ * _prime_ + _Key.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is GlobalTableKey _o_)
            {
                int _c_;
                _c_ = _Id.CompareTo(_o_._Id);
                if (_c_ != 0) return _c_;
                _c_ = _Key.CompareTo(_o_._Key);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey");
        }

        public bool NegativeCheck()
        {
            if (Id < 0) return true;
            return false;
        }
    }
}
