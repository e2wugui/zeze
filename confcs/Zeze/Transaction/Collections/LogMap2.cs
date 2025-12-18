using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogMap2<K, V> : LogMap1<K, V>
        where V : ConfBean, new()
    {
        public new static readonly string StableName = Reflect.GetStableName(typeof(LogMap2<K, V>));
        public new static readonly int TypeId_ = FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        // changed V logs. using in collect.
        // ReSharper disable once CollectionNeverUpdated.Global
        public readonly ISet<LogBean> Changed = new HashSet<LogBean>();

        // changed with key. using in encode/decode FollowerApply
        // ReSharper disable once CollectionNeverQueried.Global
        public readonly Dictionary<K, LogBean> ChangedWithKey = new Dictionary<K, LogBean>();

        public override void Decode(ByteBuffer bb)
        {
            ChangedWithKey.Clear();
            for (int i = bb.ReadUInt(); i > 0; --i)
            {
                var key = SerializeHelper<K>.Decode(bb);
                var value = DecodeLogBean(bb);
                ChangedWithKey.Add(key, value);
            }
            base.Decode(bb);
        }

        public override void Encode(ByteBuffer bb)
        {
            // 客户端本质上不需要Encode，而且BuildChangedWithKey是服务器专用的，
            // 这个宏使得代码可以在客户端编译通过，并且抛个异常避免万一使用了Encode，导致不正确的实现。
            throw new System.NotImplementedException();
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.Append(" Putted:");
            ByteBuffer.BuildString(sb, Replaced);
            sb.Append(" Removed:");
            ByteBuffer.BuildString(sb, Removed);
            sb.Append(" Changed:");
            ByteBuffer.BuildString(sb, Changed);
            return sb.ToString();
        }
    }
}
