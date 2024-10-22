// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Collections.LinkedMap
{
    [System.Serializable]
    public sealed class BLinkedMapNodeValue : Zeze.Util.ConfBean
    {
        public string Id; // LinkedMap的Key转成字符串类型
        public Zeze.Util.ConfBean Value;

        public static long GetSpecialTypeIdFromBean_2(Zeze.Util.ConfBean bean)
        {
            return Zeze.Collections.LinkedMap.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Util.ConfBean CreateBeanFromSpecialTypeId_2(long typeId)
        {
            return Zeze.Collections.LinkedMap.CreateBeanFromSpecialTypeId(typeId);
        }

        public BLinkedMapNodeValue()
        {
            Id = "";
        }

        public const long TYPEID = -6110801358414370128;
        public override long TypeId => -6110801358414370128;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeValue: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Id").Append('=').Append(Id).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Value").Append('=').Append(Environment.NewLine);
            Value?.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                string _x_ = Id;
                if (_x_ != null && _x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                var _x_ = Value;
                if (_x_ != null)
                {
                    _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.DYNAMIC);
                    _o_.WriteLong(GetSpecialTypeIdFromBean_2(_x_));
                    _x_.Encode(_o_);
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
                Id = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.DYNAMIC)
                {
                    var _x_ = CreateBeanFromSpecialTypeId_2(_o_.ReadLong());
                    _x_.Decode(_o_);
                    Value = _x_;
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "DynamicBean");
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
                    case 1: Id = ((Zeze.Transaction.Log<string>)vlog).Value; break;
                    case 2: Value.FollowerApply(vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            Id = "";
            Value.ClearParameters();
        }
    }
}
