
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Beans.Collections.Queue;
using Zeze.Transaction;
using System;

namespace Zeze.Collections
{
	public abstract class Queue
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

		public class Module : AbstractQueue
		{
			private readonly ConcurrentDictionary<string, Queue> Queues = new();

			public Module(Zeze.Application zeze)
			{
				RegisterZezeTables(zeze);
			}

			public Queue<T> Open<T>(string name, int nodeCapacity = 100)
				where T : Bean, new()
			{
				return (Queue<T>)Queues.GetOrAdd(name, key => new Queue<T>(this, key, nodeCapacity));
			}
		}
	}

	public class Queue<V> : Queue
		where V : Bean, new()
	{
		private readonly Module module;
		private readonly string name;
		private readonly int nodeCapacity;

		internal Queue(Module module, string name, int nodeSize)
		{
			this.module = module;
			this.name = name;
			this.nodeCapacity = nodeSize;
			BeanFactory.Register<V>();
		}

		public string Name => name;

		public async Task<bool> IsEmptyAsync()
		{
			var root = await module._tQueues.GetAsync(name);
			return root == null || root.HeadNodeId == 0;
		}

		/**
		 * 删除并返回整个头节点
		 *
		 * @return 头节点，null if empty
		 */
		public async Task<BQueueNode> PollNodeAsync()
		{
			var root = await module._tQueues.GetAsync(name);
			if (root == null)
				return null;

			long headNodeId = root.HeadNodeId;
			if (headNodeId == 0)
				return null;
			var nodeKey = new BQueueNodeKey(name, headNodeId);
			var head = await module._tQueueNodes.GetAsync(nodeKey);
			if (head == null)
				return null;

			root.HeadNodeId = head.NextNodeId;
			await module._tQueueNodes.DelayRemoveAsync(nodeKey);
			return head;
		}

		/**
		 * @return 头节点，null if empty
		 */
		public async Task<BQueueNode> PeekNodeAsync()
		{
			var root = await module._tQueues.GetAsync(name);
			if (root == null)
				return null;

			long headNodeId = root.HeadNodeId;
			if (headNodeId == 0)
				return null;
			return await module._tQueueNodes.GetAsync(new BQueueNodeKey(name, headNodeId));
		}

		/**
		 * 删除并返回头节点中的首个值
		 *
		 * @return 头节点的首个值，null if empty
		 */
		public async Task<V> PollAsync()
		{
			var root = await module._tQueues.GetAsync(name);
			if (root == null)
				return null;

			long headNodeId = root.HeadNodeId;
			if (headNodeId == 0)
				return null;
			var nodeKey = new BQueueNodeKey(name, headNodeId);
			var head = await module._tQueueNodes.GetAsync(nodeKey);
			if (head == null)
				return null;

			var nodeValues = head.Values;
			var nodeValue = nodeValues[0];
			nodeValues.RemoveAt(0);
			if (nodeValues.Count == 0)
			{
				root.HeadNodeId = head.NextNodeId;
				await module._tQueueNodes.RemoveAsync(nodeKey);
			}
			var value = (V)nodeValue.Value.Bean;
			return value;
		}

		/**
		 * @return 头节点的首个值，null if empty
		 */
		public async Task<V> PeekAsync()
		{
			var root = await module._tQueues.GetAsync(name);
			if (root == null)
				return null;

			long headNodeId = root.HeadNodeId;
			if (headNodeId == 0)
				return null;
			var head = await module._tQueueNodes.GetAsync(new BQueueNodeKey(name, headNodeId));
			if (head == null)
				return null;

			var value = (V)head.Values[0].Value.Bean;
			return value;
		}

		/**
		 * 用作queue, 值追加到尾节点的最后, 满则追加一个尾节点。
		 */
		public async Task AddAsync(V value)
		{
			var root = await module._tQueues.GetOrAddAsync(name);
			var tailNodeId = root.TailNodeId;
			var tail = tailNodeId != 0 ? await module._tQueueNodes.GetAsync(new BQueueNodeKey(name, tailNodeId)) : null;
			if (tail == null || tail.Values.Count >= nodeCapacity)
			{
				var newNodeId = root.LastNodeId + 1;
				root.LastNodeId = newNodeId;
				root.TailNodeId = newNodeId;
				if (root.HeadNodeId == 0)
					root.HeadNodeId = newNodeId;
				if (tail != null)
					tail.NextNodeId = newNodeId;
				await module._tQueueNodes.InsertAsync(new BQueueNodeKey(name, newNodeId), tail = new BQueueNode());
			}
			var nodeValue = new BQueueNodeValue();
			nodeValue.Timestamp = Util.Time.NowUnixMillis;
			nodeValue.Value.Bean = value;
			tail.Values.Add(nodeValue);
		}

		/**
		 * 用作stack, 值追加到头节点的首位, 满则追加一个头节点
		 */
		public async Task PushAsync(V value)
		{
			var root = await module._tQueues.GetOrAddAsync(name);
			var headNodeId = root.HeadNodeId;
			var head = headNodeId != 0 ? await module._tQueueNodes.GetAsync(new BQueueNodeKey(name, headNodeId)) : null;
			if (head == null || head.Values.Count >= nodeCapacity)
			{
				var newNodeId = root.LastNodeId + 1;
				root.LastNodeId = newNodeId;
				root.HeadNodeId = newNodeId;
				if (root.TailNodeId == 0)
					root.TailNodeId = newNodeId;
				await module._tQueueNodes.InsertAsync(new BQueueNodeKey(name, newNodeId), head = new BQueueNode());
				if (headNodeId != 0)
					head.NextNodeId = headNodeId;
			}
			var nodeValue = new BQueueNodeValue();
			nodeValue.Timestamp = Util.Time.NowUnixMillis;
			nodeValue.Value.Bean = value;
			head.Values.Insert(0, nodeValue);
		}

		/**
		 * 删除并返回头节点中的首个值
		 *
		 * @return 头节点的首个值，null if empty
		 */
		public async Task<V> PopAsync()
		{
			return await PollAsync();
		}

		// foreach

		/**
		 * 必须在事务外。
		 * func 第一个参数是当前Value所在的Node.Id。
		 */
		public async Task<long> WalkAsync(Func<long, V, bool> func)
		{
			long count = 0L;
			var root = await module._tQueues.SelectDirtyAsync(name);
			if (null == root)
				return count;
			var nodeId = root.HeadNodeId;
			while (nodeId != 0)
			{
				var node = await module._tQueueNodes.SelectDirtyAsync(new BQueueNodeKey(name, nodeId));
				if (null == node)
					return count;
				foreach (var value in node.Values)
				{
					++count;
					if (!func(nodeId, (V)value.Value.Bean))
						return count;
				}
				nodeId = node.NextNodeId;
			}
			return count;
		}
	}
}
