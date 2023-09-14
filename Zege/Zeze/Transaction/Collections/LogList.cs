namespace Zeze.Transaction.Collections
{
	// ReSharper disable once RedundantDisableWarningComment
	// ReSharper disable once UnusedTypeParameter
    public abstract class LogList<E> : LogBean
    {
#if !USE_CONFCS
		internal System.Collections.Immutable.ImmutableList<E> Value { get; set; }

		public override void Collect(Changes changes, Bean recent, Log vlog)
		{
			throw new System.NotImplementedException("Collect Not Implement.");
		}

		public override void Commit()
		{
			((CollList<E>)This)._list = Value;
		}
#endif
    }
}
