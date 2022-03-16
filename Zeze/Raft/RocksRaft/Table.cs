using RocksDbSharp;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
    public abstract class Table
    {
        public string Name { get; protected set; }
        public int CacheCapacity { get; set; } = 10000;

        internal abstract Record FollowerApply(object key, Changes.Record rlog);
        internal abstract void Open();
        public abstract Bean NewValue();
        public abstract void EncodeKey(ByteBuffer bb, object key);
        public abstract void DecodeKey(ByteBuffer bb, out object key);

        internal abstract ColumnFamilyHandle ColumnFamily { get; set; }

        public string TemplateName { get; internal set; }
        public int TemplateId { get; internal set; }
    }

    public abstract class TableTemplate
    {
        public string Name { get; }
        public Rocks Rocks { get; }

        public TableTemplate(Rocks r, string name)
        {
            Rocks = r;
            Name = name;
        }

        public abstract Table OpenTable(int templateId);

        public Table<K, V> OpenTable<K, V>(int templateId = 0)
            where V : Bean, new()
        {
            return (Table<K, V>)OpenTable(templateId); // 类型必须匹配。
        }
    }

    public class TableTemplate<K, V> : TableTemplate
        where V : Bean, new()
    {
        public TableTemplate(Rocks r, string name)
            : base(r, name)
        {

        }

        public override Table OpenTable(int templateId)
        {
            return Rocks.Tables.GetOrAdd($"{Name}#{templateId}", (key) => new Table<K, V>(Rocks, Name, templateId));
        }

        public Table<K, V> OpenTableWithType(int templateId)
        {
            return OpenTable<K, V>(templateId);
        }
    }

    public class Table<K, V> : Table where V : Bean, new()
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public override void DecodeKey(ByteBuffer bb, out object key)
        {
            key = SerializeHelper<K>.Decode(bb);
        }

        public override void EncodeKey(ByteBuffer bb, object key)
        {
            SerializeHelper<K>.Encode(bb, (K)key);
        }

        public override Bean NewValue()
        {
            return new V();
        }

        internal override Record FollowerApply(object key, Changes.Record rlog)
        {
            Record<K, V> r = null;
            switch (rlog.State)
            {
                case Changes.Record.Remove:
                    r = GetOrLoad((K)key);
                    r.Value = null;
                    r.Timestamp = Record.NextTimestamp;
                    break;

                case Changes.Record.Put:
                    r = GetOrLoad((K)key, rlog.PutValue);
                    break;

                case Changes.Record.Edit:
                    r = GetOrLoad((K)key);
                    if (null == r.Value)
                    {
                        logger.Fatal($"editting bug record not exist.");
                        Rocks.Raft.FatalKill();
                    }
                    foreach (var log in rlog.LogBean)
                        r.Value.FollowerApply(log); // 最多一个。
                    break;

                default:
                    logger.Fatal($"unknown Changes.Record.State.");
                    Rocks.Raft.FatalKill();
                    break;
            }
            return r;
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

        public bool Walk(Func<K, V, bool> callback)
        {
            using var it = Rocks.Storage.NewIterator(ColumnFamily);
            it.SeekToFirst();
            while (it.Valid())
            {
                var key = SerializeHelper<K>.Decode(ByteBuffer.Wrap(it.Key()));
                var value = SerializeHelper<V>.Decode(ByteBuffer.Wrap(it.Value()));
                if (false == callback(key, value))
                    return false;
                it.Next();
            }
            return true;
        }

        private Util.ConcurrentLruLike<K, Record<K, V>> LruCache;
        internal override ColumnFamilyHandle ColumnFamily { get; set; }
        public Rocks Rocks { get; }
        public int Capacity { get; }
        public Func<K, Record<K, V>, bool> LruTryRemoveCallback { get; set; } = null;

        public Table(Rocks rocks, string templateName, int templateId)
        {
            Rocks = rocks;
            TemplateName = templateName;
            TemplateId = templateId;

            Name = $"{TemplateName}#{TemplateId}";

            if (false == rocks.Tables.TryAdd(Name, this))
                throw new Exception($"duplicate table={Name}");

            Open();
        }

        internal override void Open()
        {
            ColumnFamily = Rocks.OpenFamily(Name);
            LruCache = new Util.ConcurrentLruLike<K, Record<K, V>>(
                    CacheCapacity, LruTryRemoveCallback, 200, 2000, 1024, Environment.ProcessorCount);
        }

        private Record<K, V> GetOrLoad(K key, Bean putvalue = null)
        {
            TableKey tkey = new TableKey(Name, key);
            while (true)
            {
                var r = LruCache.GetOrAdd(key, (_) => new Record<K, V>() { Table = this, Key = key });
                lock (r)
                {
                    if (r.Removed)
                        continue;

                    if (null != putvalue)
                    {
                        // from FollowerApply
                        r.Value = putvalue;
                        r.Value.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
                        r.Timestamp = Record.NextTimestamp;
                        r.State = Record.StateLoad;
                    }
                    else if (r.State == Record.StateNew)
                    {
                        // fresh record
                        r.Value = StorageLoad(key);
                        r.Value?.InitRootInfo(r.CreateRootInfoIfNeed(tkey), null);
                        r.Timestamp = Record.NextTimestamp;
                        r.State = Record.StateLoad;
                    }
                    // else in cache

                    return r;
                }
            }
        }

        private V StorageLoad(K key)
        {
            var keybb = ByteBuffer.Allocate();
            SerializeHelper<K>.Encode(keybb, key);
            var valuebytes = Rocks.Storage.Get(keybb.Bytes, keybb.Size, ColumnFamily);
            if (valuebytes == null)
                return null;
            var valuebb = ByteBuffer.Wrap(valuebytes);
            var value = new V();
            value.Decode(valuebb);
            return value;
        }
    }
}
