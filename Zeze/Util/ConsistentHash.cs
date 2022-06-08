
using System;
using System.Collections.Generic;

namespace Zeze.Util
{
	public class ConsistentHash<TNode>
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private int numberOfReplicas;
		private SortedDictionary<int, TNode> circle = new();
		private HashSet<TNode> nodes = new();

		public ConsistentHash()
		{
			this.numberOfReplicas = 128;
		}

		public ConsistentHash(int numberOfReplicas)
		{
			if (numberOfReplicas <= 0)
				throw new ArgumentException();
			this.numberOfReplicas = numberOfReplicas;
		}

		public IReadOnlySet<TNode> Nodes => nodes;

		public void Add(string nodeKey, TNode node)
		{
			if (null == node)
				return;

			lock (this)
			{
				if (!nodes.Add(node))
					return;

				nodeKey ??= "";
				
				for (int i = 0; i < numberOfReplicas; ++i)
				{
					var hash = Zeze.Transaction.Bean.Hash32(nodeKey + "#" + i);
					if (circle.TryAdd(hash, node))
						continue;

					if (circle.TryGetValue(hash, out var conflict))
					{
						logger.Warn($"hash conflict! add={nodeKey} i={i} exist={conflict}");
					}
				}
			}
		}

		public void Remove(string nodeKey, TNode node)
		{
			if (null == node)
				return;

			lock (this)
            {
				if (!nodes.Remove(node))
					return;

				nodeKey ??= "";

				for (int i = 0; i < numberOfReplicas; ++i)
				{
					var hash = Zeze.Transaction.Bean.Hash32(nodeKey + "#" + i);
					if (circle.TryGetValue(hash, out var current) && current.Equals(node))
						circle.Remove(hash);
				}
			}
		}

		public TNode Get(int hash)
		{
			lock (this)
            {
				throw new NotImplementedException();
				/*
				if (circle.Count == 0)
					return default;

				var tailMap = circle.TailMap(hash);
				var key = tailMap.Count == 0 ? circle.firstKey() : tailMap.firstKey();
				return circle.get(key);
				*/
			}
		}
	}
}
