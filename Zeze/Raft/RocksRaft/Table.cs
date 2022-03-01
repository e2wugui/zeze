using RocksDbSharp;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
	public abstract class Table
	{
		public string Name { get; protected set; }
		internal abstract void FollowerApply(object key, Changes.Record rlog);
        internal abstract void Open();
	}

    public class Table<K, V> : Table where V : Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        internal override void FollowerApply(object key, Changes.Record rlog)
        {
            switch (rlog.State)
            {
                case Changes.Record.Remove:
                    //ApplyRemove((K)key);
                    break;

                case Changes.Record.Put:
                    foreach (var log in rlog.LogBean) // 最多一个。
                        log.FollowerApply(rlog.PutValue);
                    //ApplyPut(rlog.PutValue);
                    break;

                case Changes.Record.Edit:
                    var r = GetOrLoad((K)key);
                    if (null == r.Value)
                    {
                        logger.Fatal($"there must be a bug.");
                        //Raft.FatalKill();
                    }
                    foreach (var log in rlog.LogBean) // 最多一个。
                        log.FollowerApply(r.Value);
                    //ApplyPut(r.Value);
                    break;
            }
        }

        public V GetOrAdd(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Name, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                V crv = (V)cr.NewestValue();
                if (null != crv)
                {
                    return crv;
                }
                // add
            }
            else
            {
                Record<K, V> r = GetOrLoad(key);
                cr = new Transaction.RecordAccessed(r);
                currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);

                if (null != r.Value)
                    return (V)r.Value;
                // add
            }

            V add = new V();
            add.InitRootInfo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, add);
            return add;
        }

        public V Get(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Name, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                return (V)cr.NewestValue();
            }

            Record<K, V> r = GetOrLoad(key);
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), new Transaction.RecordAccessed(r));
            return (V)r.Value;
        }

        public bool TryAdd(K key, V value)
        {
            if (null != Get(key))
                return false;

            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Name, key);
            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            value.InitRootInfo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
            cr.Put(currentT, value);
            return true;
        }

        public void Insert(K key, V value)
        {
            if (false == TryAdd(key, value))
                throw new ArgumentException($"table:{GetType().FullName} insert key:{key} exists");
        }

        public void Put(K key, V value)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Name, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                value.InitRootInfo(cr.Origin.CreateRootInfoIfNeed(tkey), null);
                cr.Put(currentT, value);
                return;
            }
            Record<K, V> r = GetOrLoad(key);
            cr = new Transaction.RecordAccessed(r);
            cr.Put(currentT, value);
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
        }

        // 几乎和Put一样，还是独立开吧。
        public void Remove(K key)
        {
            Transaction currentT = Transaction.Current;
            TableKey tkey = new TableKey(Name, key);

            Transaction.RecordAccessed cr = currentT.GetRecordAccessed(tkey);
            if (null != cr)
            {
                cr.Put(currentT, null);
                return;
            }

            Record<K, V> r = GetOrLoad(key);
            cr = new Transaction.RecordAccessed(r);
            cr.Put(currentT, null);
            currentT.AddRecordAccessed(r.CreateRootInfoIfNeed(tkey), cr);
        }

        private Util.ConcurrentLruLike<K, Record<K, V>> LruCache;
        private ColumnFamilyHandle ColumnFamily;
        public Rocks Rocks { get; }
        public int Capacity { get; }

        public Table(Rocks rocks, string name, int capacity)
        {
            Rocks = rocks;
            Name = name;
            Capacity = capacity;
            Open();
        }

        internal override void Open()
        {
            ColumnFamily = Rocks.OpenFamily(Name);
            LruCache = new Util.ConcurrentLruLike<K, Record<K, V>>(
                    Capacity, LruTryRemoveCallback, 200, 2000, 1024, Environment.ProcessorCount);
        }

        private bool LruTryRemoveCallback(K key, Record<K, V> r)
        {
            lock (r)
            {
                r.Removed = true;
                return LruCache.TryRemove(key, out _);
            }
        }

        private Record<K, V> GetOrLoad(K key)
        {
            TableKey tkey = new TableKey(Name, key);
            while (true)
            {
                var r = LruCache.GetOrAdd(key, (_) => new Record<K, V>());
                lock (r)
                {
                    if (r.Removed)
                        continue;
                    r.Timestamp = Record.NextTimestamp;
                    r.Value = Load(key);
                    if (null != r.Value)
                    {
                        r.Value.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
                    }
                    return r;
                }
            }
        }

        private V Load(K key)
        {
            return null;
        }
	}
}
