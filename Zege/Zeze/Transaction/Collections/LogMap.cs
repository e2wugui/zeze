using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public abstract class LogMap<K, V> : LogBean
	{
#if !USE_CONFCS
		internal ImmutableDictionary<K, V> Value { get; set; }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new NotImplementedException($"Collect Not Implement.");
		}

		public override void Commit()
		{
			((CollMap<K, V>)This)._map = Value;
		}
#endif
	}
}
