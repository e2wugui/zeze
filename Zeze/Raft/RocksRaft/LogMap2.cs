using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
    public class LogMap2<K, V> : LogMap1<K, V>
		where V : Bean, new()
	{
		// changed V logs
		public ISet<LogBean> Changed { get; } = new HashSet<LogBean>();

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
        }

		public override void Collect(Changes changes, RocksRaft.Record.RootInfo root, Log vlog)
		{
			if (Changed.Add((LogBean)vlog))
            {
				changes.Collect(root, this);
			}
		}

		public override string ToString()
		{
			var sb = new StringBuilder();
			sb.Append("Putted:\n");
			ByteBuffer.BuildString(sb, Putted);
			sb.Append("Removed:\n");
			ByteBuffer.BuildString(sb, Removed);
			sb.Append("Changed:\n");
			ByteBuffer.BuildString(sb, Changed);
			return sb.ToString();
		}
	}
}
