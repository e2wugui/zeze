// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Notify
{
    [System.Serializable]
    public sealed class BGetNotifyNode : Zeze.Util.ConfBean
    {
        public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey NodeKey;
        public Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode Node;

        public BGetNotifyNode()
        {
            NodeKey = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey();
            Node = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode();
        }

        public const long TYPEID = -548292283393969784;
        public override long TypeId => -548292283393969784;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Notify.BGetNotifyNode: {").Append(Environment.NewLine);
            level += 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append("NodeKey").Append('=').Append(Environment.NewLine);
            NodeKey.BuildString(sb, level + 4);
            sb.Append(',').Append(Environment.NewLine);
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
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 1, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                NodeKey.Encode(_o_);
                if (_b_ + 1 == _o_.WriteIndex)
                    _o_.WriteIndex = _a_;
                else
                    _i_ = _j_;
            }
            {
                int _a_ = _o_.WriteIndex;
                int _j_ = _o_.WriteTag(_i_, 2, ByteBuffer.BEAN);
                int _b_ = _o_.WriteIndex;
                Node.Encode(_o_);
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
                _o_.ReadBean(NodeKey, _t_);
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
                    case 1: NodeKey = ((Zeze.Transaction.Log<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey>)vlog).Value; break;
                    case 2: Zeze.Transaction.Collections.CollApply.ApplyOne<Zeze.Builtin.Collections.LinkedMap.BLinkedMapNode>(ref Node, vlog); break;
                }
            }
        }

        public override void ClearParameters()
        {
            NodeKey = new Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey();
            Node.ClearParameters();
        }
    }
}
