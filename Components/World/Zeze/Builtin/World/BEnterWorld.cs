// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.World
{
    [System.Serializable]
    public sealed class BEnterWorld : Zeze.Util.ConfBean
    {
        public int MapId;
        public Zeze.Serialize.Vector3 Position;
        public System.Collections.Generic.List<Zeze.Builtin.World.BAoiOperates> PriorityData; // 优先数据，服务器第一次进入的时候就跟随EnterWorld发送给客户端。

        public BEnterWorld()
        {
            Position = new Zeze.Serialize.Vector3();
            PriorityData = new System.Collections.Generic.List<Zeze.Builtin.World.BAoiOperates>();
        }

        public const long TYPEID = -4883142059980084950;
        public override long TypeId => -4883142059980084950;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zeze.Builtin.World.BEnterWorld: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("MapId").Append('=').Append(MapId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Position").Append("=(").Append(Position.x).Append(',').Append(Position.y).Append(',').Append(Position.z).Append(')').Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("PriorityData").Append("=[").Append(Environment.NewLine);
            level += 4;
            foreach (var Item in PriorityData)
            {
                sb.Append(Zeze.Util.Str.Indent(level)).Append("Item").Append('=').Append(Environment.NewLine);
                Item.BuildString(sb, level + 4);
                sb.Append(',').Append(Environment.NewLine);
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
                var _x_ = PriorityData;
                int _n_ = _x_.Count;
                if (_n_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 3, ByteBuffer.LIST);
                    _o_.WriteListType(_n_, ByteBuffer.BEAN);
                    foreach (var _v_ in _x_)
                    {
                        _v_.Encode(_o_);
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
                var _x_ = PriorityData;
                _x_.Clear();
                if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST)
                {
                    for (int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte()); _n_ > 0; _n_--)
                    {
                        _x_.Add(_o_.ReadBean(new Zeze.Builtin.World.BAoiOperates(), _t_));
                    }
                }
                else
                    _o_.SkipUnknownFieldOrThrow(_t_, "Collection");
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
            PriorityData.Clear();
        }
    }
}
