// auto-generated
using System;
using Zeze.Serialize;

namespace Zeze.Beans.GlobalCacheManagerWithRaft
{
    public sealed class GlobalTableKey : Serializable, System.IComparable
    {
        string _TableName;
        Zeze.Net.Binary _Key;

        // for decode only
        public GlobalTableKey()
        {
        }

        public GlobalTableKey(string _TableName_, Zeze.Net.Binary _Key_)
        {
            this._TableName = _TableName_;
            this._Key = _Key_;
        }

        public string TableName => _TableName;
        public Zeze.Net.Binary Key => _Key;

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("TableName").Append('=').Append(TableName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = _TableName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
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
                _TableName = _o_.ReadString(_t_);
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
                if (!_TableName.Equals(_obj_._TableName)) return false;
                if (!_Key.Equals(_obj_._Key)) return false;
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            const int _prime_ = 31;
            int _h_ = 0;
            _h_ = _h_ * _prime_ + _TableName.GetHashCode();
            _h_ = _h_ * _prime_ + _Key.GetHashCode();
            return _h_;
        }

        public int CompareTo(object _o1_)
        {
            if (_o1_ == this) return 0;
            if (_o1_ is GlobalTableKey _o_)
            {
                int _c_;
                _c_ = _TableName.CompareTo(_o_._TableName);
                if (_c_ != 0) return _c_;
                _c_ = _Key.CompareTo(_o_._Key);
                if (_c_ != 0) return _c_;
                return _c_;
            }
            throw new Exception("CompareTo: another object is not Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey");
        }

        public bool NegativeCheck()
        {
            return false;
        }
    }
}
