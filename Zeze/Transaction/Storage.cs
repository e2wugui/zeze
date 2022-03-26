using System;
using System.Collections.Generic;
using System.Text;
using System.Collections.Concurrent;
using Zeze.Serialize;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public abstract class Storage
    {
		public Database.TableAsync TableAsync { get; protected set; }

        public abstract int EncodeN();

		public abstract int Encode0();

		public abstract int Snapshot();

		public abstract Task<int> Flush(Database.ITransaction t);

		public abstract Task Cleanup();

        public abstract void Close();
	}

    public sealed class Storage<K, V> : Storage where V : Bean, new()
    {
        public Table Table { get; }

        public Storage(Table<K, V> table, Database database, string tableName)
        {
            Table = table;
            TableAsync = database.OpenTable(tableName);
        }

        private readonly ConcurrentDictionary<K, Record<K, V>> changed = new();
        private readonly ConcurrentDictionary<K, Record<K, V>> encoded = new();
        private readonly ConcurrentDictionary<K, Record<K, V>> snapshot = new();

        internal void OnRecordChanged(Record<K, V> r)
        {
            changed[r.Key] = r;
        }

        /*
         * Not Need Now. See Record.Dirty
        internal bool IsRecordChanged(K key)
        {
            if (changed.TryGetValue(key, out var _))
                return true;
            if (encoded.TryGetValue(key, out var _))
                return true;
            return false;
        }
        */

        /// <summary>
        /// 仅在 Checkpoint 中调用，同时只有一个线程执行。
        /// 没有得到任何锁。
        /// </summary>
        /// <returns></returns>
        public override int EncodeN()
        {
            int c = 0;
            foreach (var e in changed)
            {
                if (e.Value.TryEncodeN(changed, encoded))
                    ++c;
            }
            return c;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
        /// </summary>
        /// <returns></returns>
        public override int Encode0()
        {
            foreach (var e in changed)
            {
                e.Value.Encode0();
                encoded[e.Key] = e.Value;
            }
            int cc = changed.Count;
            changed.Clear();
            return cc;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用，在 flushWriteLock 下执行。
        /// </summary>
        /// <returns></returns>
        public override int Snapshot()
        {
            // 如果上一次checkpoint写到数据库失败，这里需要合并新的修改集。
            foreach (var e in encoded)
            {
                snapshot.TryAdd(e.Key, e.Value); // key 相同的时候，实际上记录也是相同的。TryAdd快一点。
            }
            encoded.Clear();
            int cc = snapshot.Count;
            return cc;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用。
        /// 没有拥有任何锁。
        /// </summary>
        /// <returns></returns>
        public override async Task<int> Flush(Database.ITransaction t)
        {
            foreach (var e in snapshot)
            {
                await e.Value.Flush(t);
            }
            return snapshot.Count;
        }

        /// <summary>
        /// 仅在 Checkpoint 中调用。
        /// 没有拥有任何锁。
        /// </summary>
        public override async Task Cleanup()
        {
            foreach (var e in snapshot)
            {
                await e.Value.Cleanup();
            }
            snapshot.Clear();
        }

        public async Task<V> FindAsync(K key, Table<K, V> table)
        {
            ByteBuffer value = await TableAsync.FindAsync(table.EncodeKey(key));
            return null != value ? table.DecodeValue(value) : null;
        }

        public override void Close()
        {
            TableAsync.Close();
        }
    }
}
