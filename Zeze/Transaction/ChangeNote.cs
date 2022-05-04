using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Immutable;

namespace Zeze.Transaction
{
    public abstract class ChangeNote
    {
        internal abstract Bean Bean { get; }

        internal abstract void Merge(ChangeNote other);

        internal virtual void SetChangedValue(Util.IdentityHashMap<Bean, Bean> values) // only ChangeNoteMap2 need
        {
        }
    }

    /// <summary>
    /// 不精确的 Map 改变细节通告。改变分成两个部分访问：Replaced（add or put）Removed。
    /// 直接改变 Map.Value 的数据细节保存在内部变量 ChangedValue 中，需要调用 MergeChangedToReplaced 合并到 Replaced 中。
    /// 1. 由于添加以后再删除，Removed 这里可能存在一开始不存在的项。
    /// </summary>
    /// <typeparam name="K"></typeparam>
    /// <typeparam name="V"></typeparam>
    public class ChangeNoteMap1<K, V> : ChangeNote
    {
        public Dictionary<K, V> Replaced { get; } = new Dictionary<K, V>();
        public HashSet<K> Removed { get; } = new HashSet<K>(); // 由于添加以后再删除，这里可能存在一开始不存在的项。

        protected Collections.PMap<K, V> Map { get; set; }

        internal override Bean Bean => Map;

        public ChangeNoteMap1(Collections.PMap<K, V> map)
        {
            Map = map;
        }

        /// 由于不需要 Note 来支持回滚，这里只保留最新的改变即可。
        internal void LogPut(K key, V value)
        {
            Replaced[key] = value;
            Removed.Remove(key);
        }

        /// 由于不需要 Note 来支持回滚，这里只保留最新的改变即可。
        internal void LogRemove(K key)
        {
            Removed.Add(key);
            Replaced.Remove(key);
        }

        internal override void Merge(ChangeNote note)
        {
            ChangeNoteMap1<K, V> another = (ChangeNoteMap1<K, V>)note;
            // Put,Remove 需要确认有没有顺序问题
            // this: replace 1,3 remove 2,4 nest: replace 2 remove 1
            foreach (var e in another.Replaced) LogPut(e.Key, e.Value); // replace 1,2,3 remove 4
            foreach (var e in another.Removed) LogRemove(e); // replace 2,3 remove 1,4
        }
    }

    public sealed class ChangeNoteMap2<K, V> : ChangeNoteMap1<K, V> where V : Bean, new()
    {
        // 记录 map 中的 value 发生了改变。需要查找原 Map 才能映射到 Replaced 中。
        // Notify 的时候由 Collector 设置。
        private Util.IdentityHashMap<Bean, Bean> ChangedValue;

        public ChangeNoteMap2(Collections.PMap<K, V> map) : base(map)
        {
        }

        /// <summary>
        /// 使用 Replaced 之前调用这个方法把 Map 中不是增删，而是直接改变 value 的数据合并到 Replaced 之中。
        /// </summary>
        public void MergeChangedToReplaced(Collections.PMap2<K, V> map)
        {
            if (null == ChangedValue || ChangedValue.Count == 0)
                return;

            foreach (var e in map)
            {
                if (ChangedValue.ContainsKey(e.Value))
                {
                    if (!Replaced.ContainsKey(e.Key))
                       Replaced.Add(e.Key, e.Value);
                }
            }

            ChangedValue.Clear();
        }

        internal override void SetChangedValue(Util.IdentityHashMap<Bean, Bean> values)
        {
            ChangedValue = values;
        }
    }

    public sealed class ChangeNoteSet<K> : ChangeNote
    {
        public HashSet<K> Added { get; } = new HashSet<K>();
        public HashSet<K> Removed { get; } = new HashSet<K>(); // 由于添加以后再删除，这里可能存在一开始不存在的项。

        private Collections.PSet<K> Set;
        internal override Bean Bean => Set;

        public ChangeNoteSet(Collections.PSet<K> set)
        {
            Set = set;
        }

        internal void LogAdd(K key)
        {
            Added.Add(key);
            Removed.Remove(key);
        }

        internal void LogRemove(K key)
        {
            Removed.Add(key);
            Added.Remove(key);
        }

        internal override void Merge(ChangeNote other)
        {
            ChangeNoteSet<K> another = (ChangeNoteSet<K>)other;
            // Put,Remove 需要确认有没有顺序问题
            // this: add 1,3 remove 2,4 nest: add 2 remove 1
            foreach (var e in another.Added) LogAdd(e); // replace 1,2,3 remove 4
            foreach (var e in another.Removed) LogRemove(e); // replace 2,3 remove 1,4
        }
    }
}
