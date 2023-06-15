using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{


	public class LogList2<E> : LogList1<E>
#if USE_CONFCS
			where E : Util.ConfBean, new()
#else
            where E : Bean, new()
#endif
	{
        public readonly static new string StableName = Util.Reflect.GetStableName(typeof(LogList2<E>));
        public readonly static new int TypeId_ = Util.FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

        public class OutInt
		{
			public int Value { get; set; }
            public override string ToString()
            {
                return Value.ToString();
            }
        }

		public Dictionary<LogBean, OutInt> Changed { get; } = new(); // changed V logs. using in collect.

#if !USE_CONFCS
		internal override Log BeginSavepoint()
		{
			var dup = new LogList2<E>();
            dup.This = This;
            dup.Belong = Belong;
			dup.VariableId = VariableId;
			dup.Value = Value;
			return dup;
		}

        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            if (Changed.TryAdd((LogBean)vlog, new OutInt()))
                changes.Collect(recent, this);
        }

#endif

        public override void Encode(ByteBuffer bb)
		{
#if USE_CONFCS
            throw new NotImplementedException();
#else
            if (null != Value)
            {
				// follower接收到log时，Value为空，此时不做Changed过滤。
				var miss = new List<LogBean>();
				foreach (var e in Changed)
				{
					var logBean = e.Key;
					var idxExist = Value.IndexOf((E)logBean.This);
					if (idxExist < 0)
						miss.Add(logBean);
					else
						e.Value.Value = idxExist;
				}
				foreach (var logbean in miss)
				{
					Changed.Remove(logbean);
				}
			}

			bb.WriteUInt(Changed.Count);
			foreach (var e in Changed)
			{
				e.Key.Encode(bb);
				bb.WriteUInt(e.Value.Value);
			}
			// encode oplogs
			base.Encode(bb);
#endif
		}

		public override void Decode(ByteBuffer bb)
		{
			Changed.Clear();
			for (int i = bb.ReadUInt(); i > 0; i--)
			{
				var value = new LogBean();
				value.Decode(bb);
				var index = bb.ReadUInt();
				Changed[value] = new OutInt() { Value = index };
			}
			// deocde oplogs
			base.Decode(bb);
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append("OpLogs:");
			ByteBuffer.BuildString(sb, OpLogs);
			sb.Append(" Changed:");
			ByteBuffer.BuildString(sb, Changed);
			return sb.ToString();
		}
	}
}
