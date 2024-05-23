// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor MergeConditionalExpression
// ReSharper disable PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.HotDistribute
{
    public interface BVariableReadOnly
    {
        public long TypeId { get; }
        public void Encode(ByteBuffer _os_);
        public bool NegativeCheck();
        public BVariable Copy();

        public int Id { get; }
        public string Name { get; }
        public string Type { get; }
        public string Key { get; }
        public string Value { get; }
    }

    public sealed class BVariable : Zeze.Transaction.Bean, BVariableReadOnly
    {
        int _Id;
        string _Name;
        string _Type;
        string _Key;
        string _Value;

        public int Id
        {
            get
            {
                if (!IsManaged)
                    return _Id;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Id;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Id)txn.GetLog(ObjectId + 1);
                return log != null ? log.Value : _Id;
            }
            set
            {
                if (!IsManaged)
                {
                    _Id = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Id() { Belong = this, VariableId = 1, Value = value });
            }
        }

        public string Name
        {
            get
            {
                if (!IsManaged)
                    return _Name;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Name;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Name)txn.GetLog(ObjectId + 2);
                return log != null ? log.Value : _Name;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Name = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Name() { Belong = this, VariableId = 2, Value = value });
            }
        }

        public string Type
        {
            get
            {
                if (!IsManaged)
                    return _Type;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Type;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Type)txn.GetLog(ObjectId + 3);
                return log != null ? log.Value : _Type;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Type = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Type() { Belong = this, VariableId = 3, Value = value });
            }
        }

        public string Key
        {
            get
            {
                if (!IsManaged)
                    return _Key;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Key;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Key)txn.GetLog(ObjectId + 4);
                return log != null ? log.Value : _Key;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Key = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Key() { Belong = this, VariableId = 4, Value = value });
            }
        }

        public string Value
        {
            get
            {
                if (!IsManaged)
                    return _Value;
                var txn = Zeze.Transaction.Transaction.Current;
                if (txn == null) return _Value;
                txn.VerifyRecordAccessed(this, true);
                var log = (Log__Value)txn.GetLog(ObjectId + 5);
                return log != null ? log.Value : _Value;
            }
            set
            {
                if (value == null) throw new System.ArgumentNullException(nameof(value));
                if (!IsManaged)
                {
                    _Value = value;
                    return;
                }
                var txn = Zeze.Transaction.Transaction.Current;
                txn.VerifyRecordAccessed(this);
                txn.PutLog(new Log__Value() { Belong = this, VariableId = 5, Value = value });
            }
        }

        public BVariable()
        {
            _Name = "";
            _Type = "";
            _Key = "";
            _Value = "";
        }

        public BVariable(int _Id_, string _Name_, string _Type_, string _Key_, string _Value_)
        {
            _Id = _Id_;
            _Name = _Name_;
            _Type = _Type_;
            _Key = _Key_;
            _Value = _Value_;
        }

        public void Assign(BVariable other)
        {
            Id = other.Id;
            Name = other.Name;
            Type = other.Type;
            Key = other.Key;
            Value = other.Value;
        }

        public BVariable CopyIfManaged()
        {
            return IsManaged ? Copy() : this;
        }

        public override BVariable Copy()
        {
            var copy = new BVariable();
            copy.Assign(this);
            return copy;
        }

        public static void Swap(BVariable a, BVariable b)
        {
            BVariable save = a.Copy();
            a.Assign(b);
            b.Assign(save);
        }

        public const long TYPEID = 7877437207710416076;
        public override long TypeId => TYPEID;

        sealed class Log__Id : Zeze.Transaction.Log<int>
        {
            public override void Commit() { ((BVariable)Belong)._Id = this.Value; }
        }

        sealed class Log__Name : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BVariable)Belong)._Name = this.Value; }
        }

        sealed class Log__Type : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BVariable)Belong)._Type = this.Value; }
        }

        sealed class Log__Key : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BVariable)Belong)._Key = this.Value; }
        }

        sealed class Log__Value : Zeze.Transaction.Log<string>
        {
            public override void Commit() { ((BVariable)Belong)._Value = this.Value; }
        }

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.HotDistribute.BVariable: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Id").Append('=').Append(Id).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Name").Append('=').Append(Name).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Type").Append('=').Append(Type).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = Id;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                string _x_ = Name;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = Type;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = Key;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = Value;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                Id = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Name = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Type = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Key = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                Value = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override bool NegativeCheck()
        {
            if (Id < 0) return true;
            return false;
        }

        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: _Id = vlog.IntValue(); break;
                    case 2: _Name = vlog.StringValue(); break;
                    case 3: _Type = vlog.StringValue(); break;
                    case 4: _Key = vlog.StringValue(); break;
                    case 5: _Value = vlog.StringValue(); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Id = 0;
            Name = "";
            Type = "";
            Key = "";
            Value = "";
        }
    }
}
