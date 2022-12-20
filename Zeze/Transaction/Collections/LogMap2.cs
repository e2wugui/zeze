using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class LogMap2<K, V> : LogMap1<K, V>
#if USE_CONFCS
		where V : Util.ConfBean, new()
#else
        where V : Bean, new()
#endif
	{
		// changed V logs. using in collect.
		public ISet<LogBean> Changed { get; } = new HashSet<LogBean>();

		// changed with key. using in encode/decode FollowerApply
		public Dictionary<K, LogBean> ChangedWithKey { get; } = new Dictionary<K, LogBean>();

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
            {
                changes.Collect(recent, this);
            }
        }

        private bool Built = false;

        public bool BuildChangedWithKey()
        {
            if (false == Built && null != Value)
            {
                Built = true;
                foreach (var c in Changed)
                {
                    if (CollMap2<K, V>.PropertyMapKey != null)
                    {
                        var pkey = (K)CollMap2<K, V>.PropertyMapKey.GetValue(c.This);
                        if (false == Replaced.ContainsKey(pkey) && false == Removed.Contains(pkey))
                            ChangedWithKey.Add(pkey, c);
                        continue;
                    }
                    // slow search.
                    foreach (var e in Value)
                    {
                        if (c.Belong == e.Value)
                        {
                            if (false == Replaced.ContainsKey(e.Key) && false == Removed.Contains(e.Key))
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
                {
                    Replaced.TryAdd(e.Key, (V)e.Value.This);
                }
            }
        }

#endif
        public override void Decode(ByteBuffer bb)
        {
			ChangedWithKey.Clear();
			for (int i = bb.ReadUInt(); i > 0; --i)
			{
				var key = SerializeHelper<K>.Decode(bb);
				var value = SerializeHelper<LogBean>.Decode(bb);
				ChangedWithKey.Add(key, value);
			}
            base.Decode(bb);
        }

		public override void Encode(ByteBuffer bb)
        {
            // 客户端本质上不需要Encode，而且BuildChangedWithKey是服务器专用的，
            // 这个宏使得代码可以在客户端编译通过，并且抛个异常避免万一使用了Encode，导致不正确的实现。
#if USE_CONFCS
            throw new NotImplementedException();
#else
            BuildChangedWithKey();
#endif
            bb.WriteUInt(ChangedWithKey.Count);
			foreach (var e in ChangedWithKey)
            {
				SerializeHelper<K>.Encode(bb, e.Key);
				SerializeHelper<LogBean>.Encode(bb, e.Value);
			}
			base.Encode(bb);
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
