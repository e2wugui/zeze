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
        public const int eStateMask = 0xf;
        public const int eStateStand = 0; // 站立，静止状态
        public const int eStateSlide = 1; // 滑动（斜坡）这是一种失控状态，滑动方向由斜坡决定
        public const int eStateFly = 2; // 空中允许转向
        public const int eStateFlyLine = 3; // 空中不允许转向
        public const int eStateSwim = 4; // 水面（游泳）
        public const int eStateSwimUnderwater = 5; // 水中（游泳）
        public const int eStateStandUnderwater = 6; // 水中（游泳）
        public const int eControlMoveMask = 0x3;
        public const int eControlMoveNone = 0;
        public const int eControlMoveForward = 1;
        public const int eControlMoveBack = 2;
        public const int eControlTurnMask = 0xc;
        public const int eControlTurnNone = 0;
        public const int eControlTurnLeft = 4;
        public const int eControlTurnRight = 8;

        public Zeze.Serialize.Vector3 Position; // 命令时刻的客户端真实位置。
        public Zeze.Serialize.Vector3 Direct; // 命令时刻的客户端真实朝向。
        public int State; // 状态
        public int Control; // 控制命令
        public long Timestamp; // 命令时刻的时戳。

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
            sb.Append(Zeze.Util.Str.Indent(level)).Append("State").Append('=').Append(State).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Control").Append('=').Append(Control).Append(',').Append(Environment.NewLine);
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
                int _x_ = State;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                int _x_ = Control;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 4, ByteBuffer.INTEGER);
                    _o_.WriteInt(_x_);
                }
            }
            {
                long _x_ = Timestamp;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 5, ByteBuffer.INTEGER);
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
                State = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 4)
            {
                Control = _o_.ReadInt(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 5)
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
            State = 0;
            Control = 0;
            Timestamp = 0;
        }
    }
}
