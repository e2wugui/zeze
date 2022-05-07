using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class LogMap2<K, V> : LogMap1<K, V>
		where V : Bean, new()
	{
		// changed V logs. using in collect.
		public ISet<LogBean> Changed { get; } = new HashSet<LogBean>();

		// changed with key. using in encode/decode FollowerApply
		public Dictionary<K, LogBean> ChangedWithKey { get; } = new Dictionary<K, LogBean>();

		internal override Log BeginSavepoint()
		{
			var dup = new LogMap2<K, V>();
			dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}

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

		private bool Merged = false;

		public void BuildChangedWithKey()
		{
			if (false == Merged && null != Value)
			{
				Merged = true;
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
			}
		}

		public void MergeChangedToReplaced()
		{
			BuildChangedWithKey();

			foreach (var e in ChangedWithKey)
			{
				Replaced.TryAdd(e.Key, (V)e.Value.This);
			}
		}

		public override void Encode(ByteBuffer bb)
        {
			BuildChangedWithKey();

			bb.WriteUInt(ChangedWithKey.Count);
			foreach (var e in ChangedWithKey)
            {
				SerializeHelper<K>.Encode(bb, e.Key);
				SerializeHelper<LogBean>.Encode(bb, e.Value);
			}
			base.Encode(bb);
        }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			if (Changed.Add((LogBean)vlog))
            {
				changes.Collect(recent, this);
			}
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
