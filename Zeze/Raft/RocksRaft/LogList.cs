
using System;
using System.Collections.Immutable;

namespace Zeze.Raft.RocksRaft
{
	public abstract class LogList<E> : LogBean
	{
		internal ImmutableList<E> Value { get; set; }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new NotImplementedException("Collect Not Implement.");
		}
	}
}
