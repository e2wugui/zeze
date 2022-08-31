using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class LogSet<V> : LogBean
	{
#if !USE_CONFCS
		internal ImmutableHashSet<V> Value { get; set; }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new Exception($"Collect Not Implement.");
		}

        public override void Commit()
        {
			((CollSet<V>)This)._set = Value;
        }
#endif
	}
}
