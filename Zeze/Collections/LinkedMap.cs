
using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Builtin.Collections.LinkedMap;
using Zeze.Transaction;

namespace Zeze.Collections
{
	public abstract class LinkedMap
	{
		internal static readonly BeanFactory BeanFactory = new BeanFactory();

		public static long GetSpecialTypeIdFromBean(Bean bean)
		{
			return bean.TypeId;
		}

		public static Bean CreateBeanFromSpecialTypeId(long typeId)
		{
			return BeanFactory.Create(typeId);
		}


		public class Module : AbstractLinkedMap
		{
			private readonly ConcurrentDictionary<string, LinkedMap> LinkedMaps = new();
			public Zeze.Application Zeze { get; }

			public Module(Zeze.Application zeze)
			{
				Zeze = zeze;
				RegisterZezeTables(zeze);
			}

            public override void UnRegister()
            {
				UnRegisterZezeTables(Zeze);
            }

            public LinkedMap<V> Open<V>(string name, int nodeCapacity = 100)
				where V : Bean, new()
			{
				return (LinkedMap<V>)LinkedMaps.GetOrAdd(name, k => new LinkedMap<V>(this, k, nodeCapacity));
			}
		}
	}

	public class LinkedMap<V> : LinkedMap
		where V : Bean, new()
	{
		private readonly Module module;
		private readonly string name;
		private readonly int nodeCapacity;

		internal LinkedMap(Module module, string name, int nodeSize)
		{
			this.module = module;
			this.name = name;
			this.nodeCapacity = nodeSize;
			BeanFactory.Register<V>();
		}

		public string Name => name;

		// list
		public async Task<BLinkedMap> GetRootAsync()
		{
			return await module._tLinkedMaps.GetAsync(name);
		}

		public async Task<BLinkedMapNode> GetNodeAsync(long nodeId)
		{
			return await module._tLinkedMapNodes.GetAsync(new BLinkedMapNodeKey(name, nodeId));
		}

		public async Task<BLinkedMapNode> GetFristNodeAsync()
		{
			var root = await GetRootAsync();
			if (null != root)
				return await GetNodeAsync(root.HeadNodeId);
			return null;
		}

		public async Task<bool> IsEmptyAsync()
		{
			return await GetSizeAsync() == 0;
		}

		public async Task<long> GetSizeAsync()
		{
			return (await GetRootAsync()).Count;
		}


		public async Task<long> MoveAheadAsync(string id)
        {
			return await MoveAsync(id, true);
        }

		public async Task<long> MoveTailAsync(string id)
		{
			return await MoveAsync(id, false);
		}

		private async Task<long> MoveAsync(string id, bool ahead)
		{
			var nodeId = await module._tValueIdToNodeId.GetAsync(new BLinkedMapKey(name, id));
			if (nodeId == null)
				return 0;

			var nodeIdLong = nodeId.NodeId;
			var node = await GetNodeAsync(nodeIdLong);
			var values = node.Values;
			int i = values.Count - 1;
			// activate。优化：这个操作比较多，并且很可能都是尾部活跃，需要判断已经是最后一个了，不调整。
			if (ahead)
            {
				if (values[0].Id.Equals(id) && (await GetRootAsync()).HeadNodeId == nodeIdLong) // HeadNode && List.Last
					return nodeIdLong;
			}
			else
            {
				if (values[i].Id.Equals(id) && (await GetRootAsync()).TailNodeId == nodeIdLong) // TailNode && List.Last
					return nodeIdLong;
			}
			for (; i >= 0; i--)
			{
				var e = values[i];
				if (e.Id.Equals(id))
				{
					values.RemoveAt(i);
					if (values.Count == 0)
						await RemoveNodeUnsafeAsync(nodeId, node);
					return ahead ? await AddHeadUnsafeAsync(e.Copy()) : await AddTailUnsafeAsync(e.Copy());
				}
			}
			throw new Exception("Node Exist But Value Not Found.");
		}

		// map
		public async Task<V> GetOrAddAsync(string id)
		{
			var value = await GetAsync(id);
			if (null != value)
				return value;
			value = new V();
			await PutAsync(id, value);
			return value;
		}

		public async Task<V> PutAsync(long id, V value)
		{
			return await PutAsync(id.ToString(), value, true);
		}

		public async Task<V> PutAsync(string id, V value)
		{
			return await PutAsync(id, value, true);
		}

		public async Task<V> PutAsync(string id, V value, bool ahead)
		{
			var nodeIdKey = new BLinkedMapKey(name, id);
			var nodeId = await module._tValueIdToNodeId.GetAsync(nodeIdKey);
			if (nodeId == null)
			{
                var newNodeValue = new BLinkedMapNodeValue
                {
                    Id = id
                };
                newNodeValue.Value.Bean = value;
                nodeId = new BLinkedMapNodeId
                {
                    NodeId = ahead ? await AddHeadUnsafeAsync(newNodeValue) : await AddTailUnsafeAsync(newNodeValue)
                };
                await module._tValueIdToNodeId.InsertAsync(nodeIdKey, nodeId);
				var root = await GetRootAsync();
				root.Count += 1;
				return null;
			}
			var node = await GetNodeAsync(nodeId.NodeId);
			foreach (var e in node.Values)
			{
				if (e.Id.Equals(id))
				{
					var old = (V)e.Value.Bean;
					e.Value.Bean = value;
					return old;
				}
			}
			throw new Exception("NodeId Exist. But Value Not Found.");
		}

		public async Task<V> GetAsync(long id)
		{
			return await GetAsync(id.ToString());
		}

		public async Task<V> GetAsync(string id)
		{
			var nodeId = await module._tValueIdToNodeId.GetAsync(new BLinkedMapKey(name, id));
			if (nodeId == null)
				return null;

			var node = await GetNodeAsync(nodeId.NodeId);
			foreach (var e in node.Values)
			{
				if (e.Id.Equals(id))
				{
					var value = (V)e.Value.Bean;
					return value;
				}
			}
			return null;
		}

		public async Task<V> RemoveAsync(long id)
		{
			return await RemoveAsync(id.ToString());
		}

		public async Task<V> RemoveAsync(string id)
		{
			var nodeKey = new BLinkedMapKey(name, id);
			var nodeId = await module._tValueIdToNodeId.GetAsync(nodeKey);
			if (nodeId == null)
				return null;

			var node = await GetNodeAsync(nodeId.NodeId);
			var values = node.Values;
			for (int i = 0, n = values.Count; i < n; i++)
			{
				var e = values[i];
				if (e.Id.Equals(id))
				{
					values.RemoveAt(i);
					await module._tValueIdToNodeId.RemoveAsync(nodeKey);
					var root = await GetRootAsync();
					root.Count -= 1;
					if (values.Count == 0)
						await RemoveNodeUnsafeAsync(nodeId, node);
					return (V)e.Value.Bean;
				}
			}
			throw new Exception("NodeId Exist. But Value Not Found.");
		}

		// foreach

		/**
		 * 必须在事务外。
		 * func 第一个参数是当前Value所在的Node.Id。
		 */
		public async Task<long> WalkAsync(Func<string, V, bool> func)
		{
			long count = 0L;
			var root = await module._tLinkedMaps.SelectDirtyAsync(name);
			if (null == root)
				return count;

			var nodeId = root.TailNodeId;
			while (nodeId != 0)
			{
				var node = await module._tLinkedMapNodes.SelectDirtyAsync(new BLinkedMapNodeKey(name, nodeId));
				if (null == node)
					return count; // error

				foreach (var value in node.Values)
				{
					++count;
					if (!func(value.Id, (V)value.Value.Bean))
						return count;
				}
				nodeId = node.PrevNodeId;
			}
			return count;
		}

		// inner
		private async Task<long> AddHeadUnsafeAsync(BLinkedMapNodeValue nodeValue)
		{
			var root = await module._tLinkedMaps.GetOrAddAsync(name);
			var headNodeId = root.HeadNodeId;
			var head = headNodeId != 0 ? await GetNodeAsync(headNodeId) : null;
			if (head != null && head.Values.Count < nodeCapacity)
			{
				// head is null means empty
				head.Values.Insert(0, nodeValue);
				return headNodeId;
			}
			var newNode = new BLinkedMapNode();
			if (headNodeId != 0)
				newNode.NextNodeId = headNodeId; // 这里包含了empty
			newNode.Values.Add(nodeValue);
			var newNodeId = root.LastNodeId + 1;
			root.LastNodeId = newNodeId;
			root.HeadNodeId = newNodeId;
			await module._tLinkedMapNodes.InsertAsync(new BLinkedMapNodeKey(name, newNodeId), newNode);
			if (head != null)
				head.PrevNodeId = newNodeId;
			else // isEmpty.
				root.TailNodeId = newNodeId;
			return newNodeId;
		}

		private async Task<long> AddTailUnsafeAsync(BLinkedMapNodeValue nodeValue)
		{
			var root = await module._tLinkedMaps.GetOrAddAsync(name);
			var tailNodeId = root.TailNodeId;
			var tail = tailNodeId != 0 ? await GetNodeAsync(tailNodeId) : null;
			if (tail != null && tail.Values.Count < nodeCapacity)
			{
				// tail is null means empty
				tail.Values.Add(nodeValue);
				return tailNodeId;
			}
			var newNode = new BLinkedMapNode();
			if (tailNodeId != 0)
				newNode.PrevNodeId = tailNodeId; // 这里包含了empty
			newNode.Values.Add(nodeValue);
			var newNodeId = root.LastNodeId + 1;
			root.LastNodeId = newNodeId;
			root.TailNodeId = newNodeId;
			await module._tLinkedMapNodes.InsertAsync(new BLinkedMapNodeKey(name, newNodeId), newNode);
			if (tail != null)
				tail.NextNodeId = newNodeId;
			else // isEmpty.
				root.HeadNodeId = newNodeId;
			return newNodeId;
		}

		private async Task RemoveNodeUnsafeAsync(BLinkedMapNodeId nodeId, BLinkedMapNode node)
		{
			var root = await GetRootAsync();
			var prevNodeId = node.PrevNodeId;
			var nextNodeId = node.NextNodeId;

			if (prevNodeId == 0) // is head
				root.HeadNodeId = nextNodeId;
			else
				(await GetNodeAsync(prevNodeId)).NextNodeId = nextNodeId;

			if (nextNodeId == 0) // is tail
				root.TailNodeId = prevNodeId;
			else
				(await GetNodeAsync(nextNodeId)).PrevNodeId = prevNodeId;

			// 没有马上删除，启动gc延迟删除。
			await module._tLinkedMapNodes.DelayRemoveAsync(new BLinkedMapNodeKey(name, nodeId.NodeId));
		}
	}
}
