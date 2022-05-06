using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class CollMap1<K, V> : CollMap<K, V>
	{
        public override V this[K key]
		{
			get => Map[key];
			set
			{
				if (value == null)
					throw new ArgumentNullException();

				if (this.IsManaged)
				{
					var txn = Transaction.Current;
					txn.VerifyRecordAccessed(this);
					var log = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
					log.SetItem(key, value);
				}
				else
				{
					_map = _map.SetItem(key, value);
				}

			}
		}

        public override void Add(K key, V value)
		{
			if (key == null)
				throw new ArgumentNullException();
			if (value == null)
				throw new ArgumentNullException(); 
			
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var maplog = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Add(key, value);
			}
			else
			{
				_map = _map.Add(key, value);
			}
		}

		public override void SetItem(K key, V value)
		{
			if (key == null)
				throw new ArgumentNullException();
			if (value == null)
				throw new ArgumentNullException(); 
			
			if (IsManaged)
            {
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var maplog = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.SetItem(key, value);
			}
			else
            {
				_map = _map.SetItem(key, value);
            }
		}

		public override bool Remove(K key)
		{
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this); 
				var maplog = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return maplog.Remove(key);
			}
			else
			{
				var newmap = _map.Remove(key);
				if (newmap == _map)
					return false;
				_map = newmap;
				return true;
			}
		}

		public override void Clear()
        {
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var maplog = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Clear();
			}
			else
			{
				_map = _map.Clear();
			}
		}

        public override void FollowerApply(Log _log)
        {
			var log = (LogMap1<K, V>)_log;
			var tmp = _map;
			tmp = tmp.SetItems(log.Putted);
			tmp = tmp.RemoveRange(log.Removed);
			_map = tmp;
		}

		public override LogBean CreateLogBean()
		{
			var log = new LogMap1<K, V>();
			log.Belong = Parent;
			log.This = this;
			log.VariableId = VariableId;
			log.Value = _map;
			return log;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{ 
		}

		public override Bean CopyBean()
		{
			var copy = new CollMap1<K, V>();
			copy._map = _map;
			return copy;
		}

        public override void Add(KeyValuePair<K, V> item)
        {
			if (item.Key == null)
				throw new ArgumentNullException();
			if (item.Value == null)
				throw new ArgumentNullException();

			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.Add(item.Key, item.Value);
			}
			else
			{
				_map = _map.Add(item.Key, item.Value);
			}
		}

		public override void AddRange(IEnumerable<KeyValuePair<K, V>> pairs)
        {
			foreach (var p in pairs)
			{
				if (p.Key == null)
					throw new ArgumentNullException();
				if (p.Value == null)
					throw new ArgumentNullException();
			}

			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.AddRange(pairs);
			}
			else
			{
				_map = _map.AddRange(pairs);
			}
		}

		public override void SetItems(IEnumerable<KeyValuePair<K, V>> items)
        {
			foreach (var p in items)
			{
				if (p.Key == null)
					throw new ArgumentNullException();
				if (p.Value == null)
					throw new ArgumentNullException();
			}

			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.SetItems(items);
			}
			else
			{
				_map = _map.SetItems(items);
			}
		}

		public override bool Remove(KeyValuePair<K, V> item)
        {
			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap1<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return log.Remove(item);
			}
			else
			{
				// equals处有box
				if (_map.TryGetValue(item.Key, out var oldv) && oldv.Equals(item.Value))
				{
					_map = _map.Remove(item.Key);
					return true;
				}
				else
				{
					return false;
				}
			}
		}
	}
}
