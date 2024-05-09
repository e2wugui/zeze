using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class CollMap2<K, V> : CollMap<K, V>
		where V : Bean, new()
	{
		public static PropertyInfo PropertyMapKey { get; }

        public override V this[K key]
		{
			get => Map[key];
			set
			{
				if (value == null)
					throw new ArgumentNullException();

				if (this.IsManaged)
				{
					value.InitRootInfoWithRedo(RootInfo, this);
					var txn = Transaction.Current;
					txn.VerifyRecordAccessed(this);
					var log = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
					log.SetItem(key, value);
				}
				else
				{
					_map = _map.SetItem(key, value);
				}
			}
		}

        static CollMap2()
		{
			var typeofV = typeof(V);

            PropertyMapKey = typeofV == typeof(DynamicBean) 
				? typeofV.GetProperty("_zeze_map_key_")
				: typeofV.GetProperty($"_zeze_map_key_{Util.Reflect.GetStableName(typeof(K))}_");
		}

        public override void ClearParameters()
        {
			Clear();
        }
       
		public override void Add(K key, V value)
		{
			if (key == null)
				throw new ArgumentNullException();
			if (value == null)
				throw new ArgumentNullException(); 
			
			PropertyMapKey?.SetValue(value, key);
			if (IsManaged)
			{
				value.InitRootInfoWithRedo(RootInfo, this);
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var maplog = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Add(key, value);
			}
			else
			{
				_map = _map.Add(key, value);
			}
		}

		public V GetOrAdd(K key)
		{
			if (false == TryGetValue(key, out var value))
				return value;
			value = new V();
			Add(key, value);
			return value;
		}

		public override void SetItem(K key, V value)
		{
			if (key == null)
				throw new ArgumentNullException();
			if (value == null)
				throw new ArgumentNullException();

			PropertyMapKey?.SetValue(value, key);
			if (IsManaged)
            {
				value.InitRootInfoWithRedo(RootInfo, this);
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var maplog = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var maplog = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var maplog = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				maplog.Clear();
			}
			else
			{
				_map = _map.Clear();
			}
		}

		public override void FollowerApply(Log _log)
		{
			var log = (LogMap2<K, V>)_log;
			var tmp = _map;
			foreach (var put in log.Replaced)
            {
				put.Value.InitRootInfo(RootInfo, this);
            }
			tmp = tmp.SetItems(log.Replaced);
			tmp = tmp.RemoveRange(log.Removed);

			// apply changed
			foreach (var e in log.ChangedWithKey)
			{
				if (tmp.TryGetValue(e.Key, out var value))
				{
					value.FollowerApply(e.Value);
				}
			}
			_map = tmp;
		}

		public override LogBean CreateLogBean()
		{
			var log = new LogMap2<K, V>
			{
				Belong = Parent,
				This = this,
				VariableId = VariableId,
				Value = _map
			};
			return log;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{
			foreach (var v in _map.Values)
			{
				v.InitRootInfo(root, this);
			}
		}

		protected override void InitChildrenRootInfoWithRedo(Record.RootInfo root)
		{
			foreach (var v in _map.Values)
			{
				v.InitRootInfoWithRedo(root, this);
			}
		}

		public override CollMap2<K, V> Copy()
		{
			var copy = new CollMap2<K, V>();
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
				item.Value.InitRootInfoWithRedo(RootInfo, this);
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.Add(item.Key, item.Value);
			}
			else
			{
				_map = _map.Add(item.Key, item.Value);
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
					p.Value.InitRootInfoWithRedo(RootInfo, this);
				}
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.AddRange(pairs);
			}
			else
			{
				_map = _map.AddRange(pairs);
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
					p.Value.InitRootInfoWithRedo(RootInfo, this);
				}
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.SetItems(pairs);
			}
			else
			{
				_map = _map.SetItems(pairs);
			}
		}

		public override bool Remove(KeyValuePair<K, V> item)
        {
			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogMap2<K, V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return log.Remove(item);
			}
			else
			{
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
