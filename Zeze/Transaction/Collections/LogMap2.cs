using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Transaction.Collections
{
    public class LogMap2<K, V> : LogMap1<K, V>
#if USE_CONFCS
        where V : ConfBean, new()
#else
        where V : Bean, new()
#endif
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

#if !USE_CONFCS
        internal override Log BeginSavepoint()
        {
            var dup = new LogMap2<K, V>();
            dup.This = This;
            dup.Belong = Belong;
            dup.VariableId = VariableId;
            dup.Value = Value;
            return dup;
        }

        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            if (Changed.Add((LogBean)vlog))
                changes.Collect(recent, this);
        }

        private bool Built = false;

        public bool BuildChangedWithKey()
        {
            if (!Built && Value != null)
            {
                Built = true;
                foreach (var c in Changed)
                {
                    if (CollMap2<K, V>.PropertyMapKey != null)
                    {
                        var pkey = (K)CollMap2<K, V>.PropertyMapKey.GetValue(c.This);
                        if (!Replaced.ContainsKey(pkey) && !Removed.Contains(pkey))
                            ChangedWithKey.Add(pkey, c);
                        continue;
                    }
                    // slow search.
                    foreach (var e in Value)
                    {
                        if (c.Belong == e.Value)
                        {
                            if (!Replaced.ContainsKey(e.Key) && !Removed.Contains(e.Key))
                                ChangedWithKey.Add(e.Key, c);
                            break;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        public void MergeChangedToReplaced()
        {
            if (BuildChangedWithKey())
            {
                foreach (var e in ChangedWithKey)
                    Replaced.TryAdd(e.Key, (V)e.Value.This);
            }
        }

#endif

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
#if USE_CONFCS
            throw new System.NotImplementedException();
#else
            BuildChangedWithKey();
            bb.WriteUInt(ChangedWithKey.Count);
            foreach (var e in ChangedWithKey)
            {
                SerializeHelper<K>.Encode(bb, e.Key);
                EncodeLogBean(bb, e.Value);
            }
            base.Encode(bb);
#endif
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
