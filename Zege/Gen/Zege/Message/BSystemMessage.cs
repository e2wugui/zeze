// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    [System.Serializable]
    public sealed class BSystemMessage : Zeze.Util.ConfBean
    {
        public System.Collections.Generic.Dictionary<string, string> Properties;

        public BSystemMessage()
        {
            Properties = new System.Collections.Generic.Dictionary<string, string>();
        }

        public const long TYPEID = 882111350196792119;
        public override long TypeId => 882111350196792119;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Message.BSystemMessage: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Properties").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var _kv_ in Properties)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append('(').Append(Environment.NewLine);
                var Key = _kv_.Key;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Key").Append('=').Append(Key).Append(',').Append(Environment.NewLine);
                var Value = _kv_.Value;
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Value).Append(',').Append(Environment.NewLine);
                sb.Append(Zeze.Util.Str.Indent(level)).Append(')').Append(Environment.NewLine);
            }
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append(']').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Properties;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.MAP);
                    _o_.WriteMapType(_n_, ByteBuffer.BYTES, ByteBuffer.BYTES);
                    foreach (var _e_ in _x_)
                    {
                        _o_.WriteString(_e_.Key);
                        _o_.WriteString(_e_.Value);
                        _n_--;
                    }
                    if (_n_ != 0)
                        throw new System.Exception(_n_.ToString());
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
                var _x_ = Properties;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.MAP)
                {
                    int _s_ = (_t_ = _o_.ReadByte()) >> ByteBuffer.TAG_SHIFT;
                    for (int _n_ = _o_.ReadUInt(); _n_ > 0; _n_--)
                    {
                        var _k_ = _o_.ReadString(_s_);
                        var _v_ = _o_.ReadString(_t_);
                        _x_.Add(_k_, _v_);
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Map");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }


        public override void FollowerApply(Zeze.Transaction.Log log)
        {
            var blog = (Zeze.Transaction.Collections.LogBean)log;
            foreach (var vlog in blog.Variables.Values)
            {
                switch (vlog.VariableId)
                {
                    case 1: Zeze.Transaction.Collections.CollApply.ApplyMap1(Properties, vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Properties.Clear();
        }
    }
}
