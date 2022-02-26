using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	/// <summary>
	/// RocksRaft 先按拥有全部功能来设想，需要分层了再考虑。
	/// 支持多个Map<K, V>。
	/// Map<K, V> 数据：LruCache - RocksDb
	/// 自由记录锁<K, V>
	/// 简单事务，一次请求（事务）一个Raft.AppendLog。这个的锁管理和自由锁需要分隔好。
	/// Raft：AppendLog，Snapshot。
	/// 和zeze事务区别：1. 仅系列化修改日志. 2. 锁和并发由应用可选，开始仅提供自由锁模式。
	/// </summary>
	public abstract class Table
	{
		public abstract void Commit(object key, BeanLog beanLog);
	}

	public class Table<K, V> : Table where V : Bean, new()
	{
		ConcurrentDictionary<K, V> table;

		public override void Commit(object key, BeanLog beanLog)
		{ 
			beanLog.Commit(table.GetOrAdd((K)key, (_) => new V()));
		}
	}

	public class Record
	{
	}

	public class RocksRaft
	{
		ConcurrentDictionary<string, Table> Tables;

		public void Commit(RocksLogs rocksLogs)
		{
			foreach (var r in rocksLogs.Records)
            {
				Tables.GetOrAdd(r.Key.Name, (_) => null).Commit(r.Key.Key, r.Value);
            }
		}
	}

	public class RocksLogs : Serializable
    {
		public Dictionary<TableKey, RecordLog> Records { get; } = new Dictionary<TableKey, RecordLog>();

        public void Decode(ByteBuffer bb)
        {
			Records.Clear();
			for (int i = bb.ReadInt(); i >= 0; i--)
            {
				var tkey = new TableKey();
				var blog = new RecordLog();
				tkey.Decode(bb);
				blog.Decode(bb);
				Records.Add(tkey, blog);
            }
        }

        public void Encode(ByteBuffer bb)
        {
			bb.WriteInt(Records.Count);
			foreach (var r in Records)
            {
				r.Key.Encode(bb);
				r.Value.Encode(bb);
            }
        }
    }

    public class TableKey : Serializable
    {
		public string Name { get; set; }
		public object Key { get; set; }

        public void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
    }

	public class Transaction
    {
		public static Transaction Current => null;

		public bool LogTryGet(long beanId, int varId, out Log log)
		{
			log = null;
			return false;
		}

		public void LogPut(long beanId, int varId, Log log)
        {

        }

		public Log LogGetOrAdd(long beanId, int varId, Func<int, Log> varLogFactory)
        {
			return varLogFactory(0);
        }
    }

    public class Bean
	{
		public long ObjectId { get; } = 0;
		public Bean Parent { get; private set; }
		public int VariableId { get; private set; }

		int _i;
		long _l;
		Map<int, int> _map;

		public int I
		{
			get
			{
				if (false == Transaction.Current.LogTryGet(this.ObjectId, 1, out var log))
					return _i;
				return ((Log_i)log).Value;
			}

			set
			{
				Transaction.Current.LogPut(this.ObjectId, 1, new Log_i(value));
			}
		}

		public Map<int, int> Map => _map;

		class Log_i : Log
		{
			public int Value { get; private set; }
			public override int VariableId => 1;
			public Log_i(int value)
            {
				Value = value;
            }
			public override void Commit(Bean holder)
			{
				holder._i = Value;
			}

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Value);
            }

            public override void Decode(ByteBuffer bb)
            {
				Value = bb.ReadInt();
            }
        }

		class Log_l : Log
		{
			public long Value { get; private set; }
			public override int VariableId => 2;
			public Log_l(long value)
            {
				Value = value;
            }
			public override void Commit(Bean holder)
			{
				holder._l = Value;
			}

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteLong(Value);
            }

            public override void Decode(ByteBuffer bb)
            {
				Value = bb.ReadLong();
            }
        }

		class Log_map : MapLog<int, int>
		{
			public override int VariableId => 3;
			public override void Commit(Bean holder)
			{
				holder._map.Commit(this);
			}
		}
	}

	public abstract class Log : Serializable
	{
		public abstract int VariableId { get; }
		public abstract void Commit(Bean holder);

		// factory ?????????
		public abstract void Encode(ByteBuffer bb);
		public abstract void Decode(ByteBuffer bb);
	}

	public abstract class BeanLog : Log
	{
		public Dictionary<int, Log> Variables { get; } = new Dictionary<int, Log>();

        public override void Commit(Bean holder)
        {
            throw new NotImplementedException();
        }

        public override void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public override void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

		public TLog Get<TLog>(int varid) where TLog : Log
        {
			if (Variables.TryGetValue(varid, out var tmp))
				return (TLog)tmp;
			return default(TLog);
        }

		public void Put(int varid, Log log)
        {
			Variables[varid] = log;
		}

		public TLog GetOrAdd<TLog>(int varid) where TLog : Log, new()
		{
			if (Variables.TryGetValue(varid, out var tmp))
				return (TLog)tmp;
			tmp = new TLog();
			Variables.Add(varid, tmp);
			return (TLog)tmp;
		}
	}

	public class RecordLog : BeanLog
    {
        public override int VariableId => throw new NotImplementedException();
    }

	public abstract class Collection : Bean
    {

    }

	public abstract class Map<K, V> : Collection
	{
		protected ImmutableDictionary<K, V> map;

		public abstract V Get(K key);
		public abstract void Put(K key, V value);
		public abstract void Remove(K key);
		public abstract void Commit(MapLog<K, V> log);

		internal V _Get(K key)
        {
			if (map.TryGetValue(key, out var tmp))
				return tmp;
			return default(V);
        }
	}

	public abstract class Map1<K, V> : Map<K, V>
	{


        public override V Get(K key)
		{
			if (false == Transaction.Current.LogTryGet(Parent.ObjectId, VariableId, out var log))
				return _Get(key);
			var maplog = (MapLog<K, V>)log;
			return maplog.Get(key, this);
		}

		public override void Put(K key, V value)
		{
			var maplog = (MapLog<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId, VariableId, MapLogFactory);
			maplog.Put(key, value);
		}

		public override void Remove(K key)
        {
			var maplog = (MapLog<K, V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId, VariableId, MapLogFactory);
			maplog.Remove(key);
		}

		public override void Commit(MapLog<K, V> log)
		{
			var tmp = map;
			tmp = tmp.RemoveRange(log.Removed);
			tmp = tmp.AddRange(log.Putted);
			map = tmp;
		}

		private Log MapLogFactory(int _)
        {
			return new MapLog<K, V>();
        }
	}

	public class MapLog<K, V> : Log
	{
		public Dictionary<K, V> Putted;
		public ISet<K> Removed;

        public override int VariableId => throw new NotImplementedException();

        public override void Commit(Bean holder)
        {
			// 在Map1,Map2中处理
			throw new NotImplementedException();
		}

		public V Get(K key, Map<K, V> map)
        {
			if (Putted.TryGetValue(key, out V value))
				return value;
			if (Removed.Contains(key))
				return default(V);
			return map._Get(key);
        }

		public void Put(K key, V value)
        {
			Putted[key] = value;
			Removed.Remove(key);
        }

		public void Remove(K key)
        {
			Putted.Remove(key);
			Removed.Add(key);
        }

        public override void Decode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }

        public override void Encode(ByteBuffer bb)
        {
            throw new NotImplementedException();
        }
    }
}
