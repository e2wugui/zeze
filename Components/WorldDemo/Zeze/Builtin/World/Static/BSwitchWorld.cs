// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.World.Static
{
    [System.Serializable]
    public sealed class BSwitchWorld : Zeze.Util.ConfBean
    {
        public int MapId;
        public Zeze.Serialize.Vector3 Position;
        public Zeze.Serialize.Vector3 Direct;

        public BSwitchWorld()
        {
            Position = new Zeze.Serialize.Vector3();
            Direct = new Zeze.Serialize.Vector3();
        }

        public const long TYPEID = -2702601729537956678;
        public override long TypeId => -2702601729537956678;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.World.Static.BSwitchWorld: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MapId").Append('=').Append(MapId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Position").Append("=(").Append(Position.x).Append(',').Append(Position.y).Append(',').Append(Position.z).Append(')').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Direct").Append("=(").Append(Direct.x).Append(',').Append(Direct.y).Append(',').Append(Direct.z).Append(')').Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                int _x_ = MapId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                Position.Encode(_o_);
            }
            {
                _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.VECTOR3);
                Direct.Encode(_o_);
            }
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            int _i_ = _o_.ReadTagSize(_t_);
            if (_i_ == 1)
            {
                MapId = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Position.Decode(_o_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Direct.Decode(_o_);
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
            MapId = 0;
            Position.Set(0, 0, 0);
            Direct.Set(0, 0, 0);
        }
    }
}
