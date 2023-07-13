// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.World
{
    [System.Serializable]
    public sealed class BObject : Zeze.Util.ConfBean
    {
        public Zeze.Util.ConfBean Data;
        public Zeze.Builtin.World.BMove Moving; // 命令型移动同步可以直接使用这个结构，如果其他模式，这里面部分变量可能未用。
        public string PlayerId;
        public string LinkName;
        public long LinkSid;
        public int Type; // 逻辑？
        public int ConfigId;

        public static long GetSpecialTypeIdFromBean_1(Zeze.Util.ConfBean bean)
        {
            return Zeze.World.World.GetSpecialTypeIdFromBean(bean);
        }

        public static Zeze.Util.ConfBean CreateBeanFromSpecialTypeId_1(long typeId)
        {
            return Zeze.World.World.CreateBeanFromSpecialTypeId(typeId);
        }

        public BObject()
        {
            Moving = new Zeze.Builtin.World.BMove();
            PlayerId = "";
            LinkName = "";
        }

        public const long TYPEID = -2457457472033861643;
        public override long TypeId => -2457457472033861643;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.World.BObject: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Data").Append('=').Append(Environment.NewLine);
            Data?.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Moving").Append('=').Append(Environment.NewLine);
            Moving.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PlayerId").Append('=').Append(PlayerId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkName").Append('=').Append(LinkName).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("LinkSid").Append('=').Append(LinkSid).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Type").Append('=').Append(Type).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("ConfigId").Append('=').Append(ConfigId).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                var _x_ = Data;
                if (_x_ != null)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.DYNAMIC);
                    _o_.WriteLong(GetSpecialTypeIdFromBean_1(_x_));
                    _x_.Encode(_o_);
                }
            }
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                Moving.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            {
                string _x_ = PlayerId;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                string _x_ = LinkName;
                if (_x_.Length != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.BYTES);
                    _o_.WriteString(_x_);
                }
            }
            {
                long _x_ = LinkSid;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                int _x_ = Type;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 6, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = ConfigId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 7, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
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
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.DYNAMIC)
                {
                    var _x_ = CreateBeanFromSpecialTypeId_1(_o_.ReadLong());
                    _x_.Decode(_o_);
                    Data = _x_;
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "DynamicBean");
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _o_.ReadBean(Moving, _t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                PlayerId = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                LinkName = _o_.ReadString(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
            {
                LinkSid = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 6)
            {
                Type = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 7)
            {
                ConfigId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }

        public override void ClearParameters()
        {
            Data.ClearParameters();
            Moving.ClearParameters();
            PlayerId = "";
            LinkName = "";
            LinkSid = 0;
            Type = 0;
            ConfigId = 0;
        }
    }
}
