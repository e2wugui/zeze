using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public abstract class LogMap : LogBean
	{
		public override void Collect(Changes changes, RocksRaft.Record.RootInfo root, Log vlog)
		{
			throw new Exception($"Collect Not Implement.");
		}
	}
}
