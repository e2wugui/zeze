// auto-generated
using ByteBuffer = Zeze.Serialize.ByteBuffer;
using Environment = System.Environment;

// ReSharper disable ConvertConstructorToMemberInitializers EmptyConstructor PossibleNullReferenceException RedundantAssignment RedundantNameQualifier
// ReSharper disable once CheckNamespace
namespace Zege.Friend
{
    [System.Serializable]
    public sealed class BGroupData : Zeze.Util.ConfBean
    {

        public BGroupData()
        {
        }

        public const long TYPEID = 3948602935785373125;
        public override long TypeId => 3948602935785373125;

        public override string ToString()
        {
            var sb = new System.Text.StringBuilder();
            BuildString(sb, 0);
            sb.Append(Environment.NewLine);
            return sb.ToString();
        }

        public override void BuildString(System.Text.StringBuilder sb, int level)
        {
            sb.Append(Zeze.Util.Str.Indent(level)).Append("Zege.Friend.BGroupData: {").Append(Environment.NewLine);
            level += 4;
            level -= 4;
            sb.Append(Zeze.Util.Str.Indent(level)).Append('}');
        }

        public override void Encode(ByteBuffer _o_)
        {
            _o_.WriteByte(0);
        }

        public override void Decode(ByteBuffer _o_)
        {
            int _t_ = _o_.ReadByte();
            _o_.ReadTagSize(_t_);
            while (_t_ != 0)
            {
                _o_.SkipUnknownField(_t_);
                _o_.ReadTagSize(_t_ = _o_.ReadByte());
            }
        }


        public override void FollowerApply(Zeze.Transaction.Log log)
        {
        }

        public override void ClearParameters()
        {
        }
    }
}
