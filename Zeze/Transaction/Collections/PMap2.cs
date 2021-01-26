using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Diagnostics;

namespace Zeze.Transaction.Collections
{
    public sealed class PMap2<K, V> : PMap<K, V> where V : Bean
    {
        public PMap2(long logKey, Func<ImmutableDictionary<K, V>, Log> logFactory) : base(logKey, logFactory)
        {
        }

        public override V this[K key]
        {
            get => Data[key];
            set
            {
                if (value == null)
                    throw new ArgumentNullException();

                if (this.IsManaged)
                {
                    value.InitTableKey(TableKey, Parent);
                    value.VariableId = this.VariableId;
                    var txn = Transaction.Current;
                    var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                    var newv = oldv.SetItem(key, value);
                    if (newv != oldv)
                    {
                        txn.PutLog(NewLog(newv));
                        ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogPut(key, value);
                    }
                }
                else
                {
                    map = map.SetItem(key, value);
                }

            }
        }

        public override void Add(K key, V value)
        {
            if (key == null)
                throw new ArgumentNullException();
            if (value == null)
                throw new ArgumentNullException();

            if (this.IsManaged)
            {
                value.InitTableKey(TableKey, Parent);
                value.VariableId = this.VariableId;
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.Add(key, value);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogPut(key, value);
                }
            }
            else
            {
                map = map.Add(key, value);
            }
        }

        public override void AddRange(IEnumerable<KeyValuePair<K, V>> pairs)
        {
            foreach (KeyValuePair<K, V> p in pairs)
            {
                if (p.Key == null)
                    throw new ArgumentNullException();
                if (p.Value == null)
                    throw new ArgumentNullException();
            }

            if (this.IsManaged)
            {
                foreach (var p in pairs)
                {
                    p.Value.InitTableKey(TableKey, Parent);
                    p.Value.VariableId = this.VariableId;
                }
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.AddRange(pairs);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteMap2<K, V> note = (ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this));
                    foreach (var p in pairs)
                    {
                        note.LogPut(p.Key, p.Value);
                    }
                }
            }
            else
            {
                map = map.AddRange(pairs);
            }
        }

        public override void SetItem(K key, V value)
        {
            if (key == null)
                throw new ArgumentNullException();
            if (value == null)
                throw new ArgumentNullException();

            if (this.IsManaged)
            {
                value.InitTableKey(TableKey, Parent);
                value.VariableId = this.VariableId;
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.SetItem(key, value);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogPut(key, value);
                }
            }
            else
            {
                map = map.SetItem(key, value);
            }
        }

        public override void SetItems(IEnumerable<KeyValuePair<K, V>> pairs)
        {
            foreach (KeyValuePair<K, V> p in pairs)
            {
                if (p.Key == null)
                    throw new ArgumentNullException();
                if (p.Value == null)
                    throw new ArgumentNullException();
            }

            if (this.IsManaged)
            {
                foreach (var p in pairs)
                {
                    p.Value.InitTableKey(TableKey, Parent);
                    p.Value.VariableId = this.VariableId;
                }
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.SetItems(pairs);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ChangeNoteMap2<K, V> note = (ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this));
                    foreach (var p in pairs)
                    {
                        note.LogPut(p.Key, p.Value);
                    }
                }
            }
            else
            {
                map = map.SetItems(pairs);
            }
        }

        public override void Add(KeyValuePair<K, V> item)
        {
            if (item.Key == null)
                throw new ArgumentNullException();
            if (item.Value == null)
                throw new ArgumentNullException();

            if (this.IsManaged)
            {
                item.Value.InitTableKey(TableKey, Parent);
                item.Value.VariableId = this.VariableId;
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.Add(item.Key, item.Value);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogPut(item.Key, item.Value);
                }
            }
            else
            {
                map = map.Add(item.Key, item.Value);
            }
        }

        public override void Clear()
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                if (!oldv.IsEmpty)
                {
                    ChangeNoteMap2<K, V> note = (ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this));
                    foreach (var e in oldv)
                    {
                        note.LogRemove(e.Key);
                    }
                    txn.PutLog(NewLog(ImmutableDictionary<K, V>.Empty));
                }
            }
            else
            {
                map = ImmutableDictionary<K, V>.Empty;
            }
        }

        public override bool Remove(K key)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                var newv = oldv.Remove(key);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogRemove(key);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var old = map;
                map = map.Remove(key);
                return old != map;
            }
        }

        public override bool Remove(KeyValuePair<K, V> item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : map;
                if (oldv.TryGetValue(item.Key, out var olde) && olde.Equals(item.Value))
                {
                    var newv = oldv.Remove(item.Key);
                    txn.PutLog(NewLog(newv));
                    ((ChangeNoteMap2<K, V>)txn.GetOrAddChangeNote(this.ObjectId, () => new ChangeNoteMap2<K, V>(this))).LogRemove(item.Key);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                if (map.TryGetValue(item.Key, out var oldv) && oldv.Equals(item.Value))
                {
                    map = map.Remove(item.Key);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }

        protected override void InitChildrenTableKey(TableKey tableKey)
        {
            foreach (var v in map.Values)
            {
                v.InitTableKey(TableKey, Parent);
            }
        }
    }
}
