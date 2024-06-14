namespace Zeze.Transaction.Collections
{
    // ReSharper disable once RedundantDisableWarningComment
    // ReSharper disable UnusedTypeParameter
    public abstract class LogMap<K, V> : LogBean
    {
#if !USE_CONFCS
        internal System.Collections.Immutable.ImmutableDictionary<K, V> Value { get; set; }

        public override void Collect(Changes changes, Bean recent, Log vlog)
        {
            throw new System.NotImplementedException($"Collect Not Implement.");
        }

        public override void Commit()
        {
            ((CollMap<K, V>)This)._map = Value;
        }
#endif
    }
    // ReSharper restore UnusedTypeParameter
}
