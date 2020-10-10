using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Immutable;

namespace Zeze.Transaction
{
    public interface ChangeNote
    {
        public void Merge(ChangeNote other);
    }

    public class ChangeNoteMap<K, V> : ChangeNote
    {
        public Dictionary<K, V> Replaced { get; } = new Dictionary<K, V>();
        public HashSet<K> Removed { get; } = new HashSet<K>(); // 由于添加以后再删除，这里可能存在一开始不存在的项。

        internal List<V> ChangedValue { get; set; } // 记录 map 中的 value 发生了改变。需要查找原 Map 才能映射到 Replaced 中。
        private Collections.PMap<K, V> Map { get; set; }

        internal ChangeNoteMap(Collections.PMap<K, V> map)
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

        public void Merge(ChangeNote note)
        {
            ChangeNoteMap<K, V> another = (ChangeNoteMap<K, V>)note;
            // TODO Put,Remove 需要确认有没有顺序问题
            // this: replace 1,3 remove 2,4 nest: repalce 2 remove 1
            foreach (var e in another.Replaced) LogPut(e.Key, e.Value); // replace 1,2,3 remove 4
            foreach (var e in another.Removed) LogRemove(e); // replace 2,3 remove 1,4
        }

        /// <summary>
        /// 使用 Replaced 之前调用这个方法把 Map 中不是增删，而是直接改变 value 的数据合并到 Replaced 之中。
        /// </summary>
        public void MergeChangedToReplaced()
        {
            if (null == ChangedValue || ChangedValue.Count == 0)
                return;

            Util.IdentityHashMap<V, V> changedMap = new Util.IdentityHashMap<V, V>();
            foreach (var change in ChangedValue)
            {
                changedMap.TryAdd(change, change);
            }

            foreach (var e in Map)
            {
                if (changedMap.ContainsKey(e.Value))
                    Replaced.TryAdd(e.Key, e.Value);
            }

            ChangedValue.Clear();
        }
    }

    // TODO 
    /*
    public class ChangeNoteSet<K> : ChangeNote
    {
    }
    */
}
