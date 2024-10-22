// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Friend
{
    [System.Serializable]
    public sealed class BGetGroupMemberNode : Zeze.Util.ConfBean
    {
        public long NodeId;
        public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode Node;

        public BGetGroupMemberNode()
        {
            Node = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode();
        }

        public const long TYPEID = 3450803674374125037;
        public override long TypeId => 3450803674374125037;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Friend.BGetGroupMemberNode: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NodeId").Append('=').Append(NodeId).Append(',').Append(Environment.NewLine);
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Node").Append('=').Append(Environment.NewLine);
            Node.BuildString(sb, level + 4);
            sb.Append(Environment.NewLine);
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            int _i_ = 0;
            {
                long _x_ = NodeId;
                if (_x_ != 0)
                {
                    _i_ = _o_.WriteTag(_i_, 1, ByteBuffer.INTEGER);
                    _o_.WriteLong(_x_);
                }
            }
            {
                var _x_ = Node;
                if (_x_ != null)
                {
                    int _a_ = _o_.WriteIndex;
                    int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
                    int _b_ = _o_.WriteIndex;
                    _x_.Encode(_o_);
                    if (_o_.WriteIndex <= _b_ + 1)
                        _o_.WriteIndex = _a_;
                    else
                        _i_ = _j_;
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
                NodeId = _o_.ReadLong(_t_);
                _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
            if (_i_ == 2)
            {
                _o_.ReadBean(Node, _t_);
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
                    case 1: NodeId = ((Zeze.Transaction.Log<long>)vlog).Value; break;
                    case 2: Zeze.Transaction.Collections.CollApply.ApplyOne<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode>(ref Node, vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            NodeId = 0;
            Node.ClearParameters();
        }
    }
}
