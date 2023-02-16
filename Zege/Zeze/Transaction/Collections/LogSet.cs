using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class LogSet<V> : LogBean
	{
        public readonly static new string StableName = Util.Reflect.GetStableName(typeof(LogSet<V>));
        public readonly static new int TypeId_ = Util.FixedHash.Hash32(StableName);

        public override int TypeId => TypeId_;

#if !USE_CONFCS
        internal System.Collections.Immutable.ImmutableHashSet<V> Value { get; set; }

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
