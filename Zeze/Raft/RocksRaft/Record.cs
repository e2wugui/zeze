using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public class Record
	{
		public class RootInfo
		{
			public Record Record { get; }
			public TableKey TableKey { get; }

			public RootInfo(Record record, TableKey tableKey)
			{
				Record = record;
				TableKey = tableKey;
			}
		}
	}
}
