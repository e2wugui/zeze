namespace Zeze.Transaction.Collections
{
    // ReSharper disable once RedundantDisableWarningComment
    // ReSharper disable UnusedTypeParameter
    /// <summary>
    /// confcs 端 SortedMap 的 Log 基类，与 <see cref="LogMap{K,V}"/> 对应。
    /// 仅作为类型分类的 marker，真正逻辑在 <see cref="LogSortedMap1{K,V}"/> 和
    /// <see cref="LogSortedMap2{K,V}"/> 里。CollApply.ApplyMap 通过 cast 到这两个
    /// 子类来应用变更。
    /// </summary>
    public abstract class LogSortedMap<K, V> : LogBean
    {
    }
    // ReSharper restore UnusedTypeParameter
}
