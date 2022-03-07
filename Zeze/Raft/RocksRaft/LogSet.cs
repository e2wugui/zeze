using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogSet<V> : LogBean
	{
		internal ImmutableHashSet<V> Value { get; set; }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new Exception($"Collect Not Implement.");
		}

	}
}
