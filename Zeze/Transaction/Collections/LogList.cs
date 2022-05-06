
using System;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
	public abstract class LogList<E> : LogBean
	{
		internal ImmutableList<E> Value
		{
			get;
			set;
		}

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new NotImplementedException("Collect Not Implement.");
		}

		public override void Commit()
		{
			((CollList<E>)This)._list = Value;
		}
	}
}
