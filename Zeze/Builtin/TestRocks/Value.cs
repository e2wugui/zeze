// auto-generated rocks
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

namespace Zeze.Builtin.TestRocks
{
    public sealed class Value : Zeze.Raft.RocksRaft.Bean
    {
        int _Int;
        bool _Bool;
        float _Float;
        double _double;
        string _String;
        Zeze.Net.Binary _Binary;
        readonly Zeze.Raft.RocksRaft.CollSet1<int> _SetInt;
        readonly Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey> _SetBeankey;
        readonly Zeze.Raft.RocksRaft.CollMap1<int, int> _MapInt;
        readonly Zeze.Raft.RocksRaft.CollMap2<int, Zeze.Builtin.TestRocks.Value> _MapBean;
        Zeze.Builtin.TestRocks.BeanKey _Beankey;

        public int _zeze_map_key_int_ { get; set; }

        public int Int
        {
            get
            {
                if (!IsManaged)
                    return _Int;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Int;
                var log = txn.GetLog(ObjectId + 1);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<int>)log).Value : _Int;
            }
            set
            {
                if (!IsManaged)
                {
                    _Int = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<int>() { Belong = this, VariableId = 1, Value = value, });
            }
        }

        public bool Bool
        {
            get
            {
                if (!IsManaged)
                    return _Bool;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Bool;
                var log = txn.GetLog(ObjectId + 2);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<bool>)log).Value : _Bool;
            }
            set
            {
                if (!IsManaged)
                {
                    _Bool = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<bool>() { Belong = this, VariableId = 2, Value = value, });
            }
        }

        public float Float
        {
            get
            {
                if (!IsManaged)
                    return _Float;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Float;
                var log = txn.GetLog(ObjectId + 3);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<float>)log).Value : _Float;
            }
            set
            {
                if (!IsManaged)
                {
                    _Float = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<float>() { Belong = this, VariableId = 3, Value = value, });
            }
        }

        public double Double
        {
            get
            {
                if (!IsManaged)
                    return _double;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _double;
                var log = txn.GetLog(ObjectId + 4);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<double>)log).Value : _double;
            }
            set
            {
                if (!IsManaged)
                {
                    _double = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<double>() { Belong = this, VariableId = 4, Value = value, });
            }
        }

        public string String
        {
            get
            {
                if (!IsManaged)
                    return _String;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _String;
                var log = txn.GetLog(ObjectId + 5);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<string>)log).Value : _String;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _String = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<string>() { Belong = this, VariableId = 5, Value = value, });
            }
        }

        public Zeze.Net.Binary Binary
        {
            get
            {
                if (!IsManaged)
                    return _Binary;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Binary;
                var log = txn.GetLog(ObjectId + 6);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<Zeze.Net.Binary>)log).Value : _Binary;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Binary = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<Zeze.Net.Binary>() { Belong = this, VariableId = 6, Value = value, });
            }
        }

        public Zeze.Raft.RocksRaft.CollSet1<int> SetInt => _SetInt;

        public Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey> SetBeankey => _SetBeankey;

        public Zeze.Raft.RocksRaft.CollMap1<int, int> MapInt => _MapInt;

        public Zeze.Raft.RocksRaft.CollMap2<int, Zeze.Builtin.TestRocks.Value> MapBean => _MapBean;

        public Zeze.Builtin.TestRocks.BeanKey Beankey
        {
            get
            {
                if (!IsManaged)
                    return _Beankey;
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                if (txn == null) return _Beankey;
                var log = txn.GetLog(ObjectId + 11);
                return log != null ? ((Zeze.Raft.RocksRaft.Log<Zeze.Builtin.TestRocks.BeanKey>)log).Value : _Beankey;
            }
            set
            {
                if (value == null)
                    throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Beankey = value;
                    return;
                }
                var txn = Zeze.Raft.RocksRaft.Transaction.Current;
                txn.PutLog(new Zeze.Raft.RocksRaft.Log<Zeze.Builtin.TestRocks.BeanKey>() { Belong = this, VariableId = 11, Value = value, });
            }
        }

        public Value()
        {
            _String = "";
            _Binary = Zeze.Net.Binary.Empty;
            _SetInt = new Zeze.Raft.RocksRaft.CollSet1<int>() { VariableId = 7 };
            _SetBeankey = new Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey>() { VariableId = 8 };
            _MapInt = new Zeze.Raft.RocksRaft.CollMap1<int, int>() { VariableId = 9 };
            _MapBean = new Zeze.Raft.RocksRaft.CollMap2<int, Zeze.Builtin.TestRocks.Value>() { VariableId = 10 };
            _Beankey = new Zeze.Builtin.TestRocks.BeanKey();
        }

        public Value(int _Int_, bool _Bool_, float _Float_, double _double_, string _String_, Zeze.Net.Binary _Binary_, Zeze.Builtin.TestRocks.BeanKey _Beankey_)
        {
            _Int = _Int_;
            _Bool = _Bool_;
            _Float = _Float_;
            _double = _double_;
            _String = _String_;
            _Binary = _Binary_;
            _SetInt = new Zeze.Raft.RocksRaft.CollSet1<int>() { VariableId = 7 };
            _SetBeankey = new Zeze.Raft.RocksRaft.CollSet1<Zeze.Builtin.TestRocks.BeanKey>() { VariableId = 8 };
            _MapInt = new Zeze.Raft.RocksRaft.CollMap1<int, int>() { VariableId = 9 };
            _MapBean = new Zeze.Raft.RocksRaft.CollMap2<int, Zeze.Builtin.TestRocks.Value>() { VariableId = 10 };
            _Beankey = _Beankey_;
        }

        public void Assign(Value other)
        {
            Int = other.Int;
            Bool = other.Bool;
            Float = other.Float;
            Double = other.Double;
            String = other.String;
            Binary = other.Binary;
            SetInt.Clear();
            foreach (var e in other.SetInt)
                SetInt.Add(e);
            SetBeankey.Clear();
            foreach (var e in other.SetBeankey)
                SetBeankey.Add(e);
            MapInt.Clear();
            foreach (var e in other.MapInt)
                MapInt.Add(e.Key, e.Value);
            MapBean.Clear();
            foreach (var e in other.MapBean)
                MapBean.Add(e.Key, e.Value);
            Beankey = other.Beankey;
        }

        public Value CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public Value Copy()
        {
            var copy = new Value();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(Value a, Value b)
        {
            Value save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public override Zeze.Raft.RocksRaft.Bean CopyBean()
        {
            return Copy();
        }

        public const long TYPEID = 7725276190606291579;
        public override long TypeId => TYPEID;

        public override string ToString()
        {
            System.Text.StringBuilder sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.TestRocks.Value: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Int").Append('=').Append(Int).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Bool").Append('=').Append(Bool).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Float").Append('=').Append(Float).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Double").Append('=').Append(Double).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("String").Append('=').Append(String).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Binary").Append('=').Append(Binary).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SetInt").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in SetInt)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Item).Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("SetBeankey").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in SetBeankey)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Environment.NewLine);
                Item.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MapInt").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in MapInt)
            {
                sb.Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(',').Append(Environment.NewLine);
                sb.Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MapBean").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in MapBean)
            {
                sb.Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
                Value.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
                sb.Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Beankey").Append('=').Append(Environment.NewLine);
            Beankey.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = Int;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                bool _x_ = Bool;
                if (_x_)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.INTEGER);
                    _o_.WriteByte(1);
                }
            }
            {
                float _x_ = Float;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.FLOAT);
                    _o_.WriteFloat(_x_);
                }
            }
            {
                double _x_ = Double;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.DOUBLE);
                    _o_.WriteDouble(_x_);
                }
            }
            {
                string _x_ = String;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Binary;
                if (_x_.Count != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.BYTES);
                    _o_.WriteBinary(_x_);
                }
            }
            {
                var _x_ = SetInt;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.INTEGER);
                    foreach (var _v_ in _x_)
                        _o_.WriteLong(_v_);
                }
            }
            {
                var _x_ = SetBeankey;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 8, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BEAN);
                    foreach (var _v_ in _x_)
                        _v_.Encode(_o_);
                }
            }
            {
                var _x_ = MapInt;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 9, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.INTEGER);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteLong(_e_.Key);
                        _o_.WriteLong(_e_.Value);
                    }
                }
            }
            {
                var _x_ = MapBean;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 10, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.INTEGER, ByteBuffer.BEAN);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteLong(_e_.Key);
                        _e_.Value.Encode(_o_);
                    }
                }
            }
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 11, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                Beankey.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                Int = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Bool = _o_.ReadBool(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Float = _o_.ReadFloat(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Double = _o_.ReadDouble(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                String = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                Binary = _o_.ReadBinary(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                var _x_ = SetInt;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadInt(_t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 8)
            {
                var _x_ = SetBeankey;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                        _x_.Add(_o_.ReadBean(new Zeze.Builtin.TestRocks.BeanKey(), _t_));
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 9)
            {
                var _x_ = MapInt;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadInt(_s_);
                        var _v_ = _o_.ReadInt(_t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 10)
            {
                var _x_ = MapBean;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadInt(_s_);
                        var _v_ = _o_.ReadBean(new Zeze.Builtin.TestRocks.Value(), _t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownField(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 11)
            {
                _o_.ReadBean(Beankey, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        protected override void InitChildrenRootInfo(Zeze.Raft.RocksRaft.Record.RootInfo root)
        {
            _SetInt.InitRootInfo(root, this);
            _SetBeankey.InitRootInfo(root, this);
            _MapInt.InitRootInfo(root, this);
            _MapBean.InitRootInfo(root, this);
        }

        public override void LeaderApplyNoRecursive(Zeze.Raft.RocksRaft.Log vlog)
        {
            switch (vlog.VariableId)
            {
                case 1: _Int = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
                case 2: _Bool = ((Zeze.Raft.RocksRaft.Log<bool>)vlog).Value; break;
                case 3: _Float = ((Zeze.Raft.RocksRaft.Log<float>)vlog).Value; break;
                case 4: _double = ((Zeze.Raft.RocksRaft.Log<double>)vlog).Value; break;
                case 5: _String = ((Zeze.Raft.RocksRaft.Log<string>)vlog).Value; break;
                case 6: _Binary = ((Zeze.Raft.RocksRaft.Log<Zeze.Net.Binary>)vlog).Value; break;
                case 7: _SetInt.LeaderApplyNoRecursive(vlog); break;
                case 8: _SetBeankey.LeaderApplyNoRecursive(vlog); break;
                case 9: _MapInt.LeaderApplyNoRecursive(vlog); break;
                case 10: _MapBean.LeaderApplyNoRecursive(vlog); break;
                case 11: _Beankey = ((Zeze.Raft.RocksRaft.Log<Zeze.Builtin.TestRocks.BeanKey>)vlog).Value; break;
            }
        }

        public override void FollowerApply(Zeze.Raft.RocksRaft.Log log)
        {
            var blog = (Zeze.Raft.RocksRaft.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Int = ((Zeze.Raft.RocksRaft.Log<int>)vlog).Value; break;
                    case 2: _Bool = ((Zeze.Raft.RocksRaft.Log<bool>)vlog).Value; break;
                    case 3: _Float = ((Zeze.Raft.RocksRaft.Log<float>)vlog).Value; break;
                    case 4: _double = ((Zeze.Raft.RocksRaft.Log<double>)vlog).Value; break;
                    case 5: _String = ((Zeze.Raft.RocksRaft.Log<string>)vlog).Value; break;
                    case 6: _Binary = ((Zeze.Raft.RocksRaft.Log<Zeze.Net.Binary>)vlog).Value; break;
                    case 7: _SetInt.FollowerApply(vlog); break;
                    case 8: _SetBeankey.FollowerApply(vlog); break;
                    case 9: _MapInt.FollowerApply(vlog); break;
                    case 10: _MapBean.FollowerApply(vlog); break;
                    case 11: _Beankey = ((Zeze.Raft.RocksRaft.Log<Zeze.Builtin.TestRocks.BeanKey>)vlog).Value; break;
                }
            }
        }

    }
}
