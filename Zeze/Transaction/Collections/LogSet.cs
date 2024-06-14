namespace Zeze.Transaction.Collections
{
    // ReSharper disable once RedundantDisableWarningComment
    // ReSharper disable once UnusedTypeParameter
    public class LogSet<V> : LogBean
    {
#if !USE_CONFCS
        internal System.Collections.Immutable.ImmutableHashSet<V> Value { get; set; }

        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            throw new System.Exception($"Collect Not Implement.");
        }

        public override void Commit()
        {
            ((CollSet<V>)This)._set = Value;
        }
#endif
    }
}
