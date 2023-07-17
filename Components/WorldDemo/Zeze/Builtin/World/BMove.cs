// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// MoveMmo
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.World
{
    [System.Serializable]
    public sealed class BMove : Zeze.Util.ConfBean
    {
        public Zeze.Serialize.Vector3 Position; // 移动命令时客户端的真实位置。
        public Zeze.Serialize.Vector3 Direct; // 移动命令时客户端真实的朝向。
        public int Command; // 0 直线，1 后退，2 左转，3 右转，4 停止。
        public long Timestamp; // 命令发起时刻的时戳。

        public BMove()
        {
            Position = new Zeze.Serialize.Vector3();
            Direct = new Zeze.Serialize.Vector3();
        }

        public const long TYPEID = 5823156345754273331;
        public override long TypeId => 5823156345754273331;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.World.BMove: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Position").Append("=(").Append(Position.x).Append(',').Append(Position.y).Append(',').Append(Position.z).Append(')').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Direct").Append("=(").Append(Direct.x).Append(',').Append(Direct.y).Append(',').Append(Direct.z).Append(')').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Command").Append('=').Append(Command).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Timestamp").Append('=').Append(Timestamp).Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.VECTOR3);
                Position.Encode(_o_);
            }
            {
                _i_ = _o_.WriteTag(_i_, 2, ByteBuffer.VECTOR3);
                Direct.Encode(_o_);
            }
            {
                int _x_ = Command;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = Timestamp;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
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
                Position.Decode(_o_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                Direct.Decode(_o_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 3)
            {
                Command = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Timestamp = _o_.ReadLong(_t_);
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
            Position.Set(0, 0, 0);
            Direct.Set(0, 0, 0);
            Command = 0;
            Timestamp = 0;
        }
    }
}
