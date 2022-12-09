
using Scriban.Syntax;
using System;
using System.Collections.Generic;
using System.Security.Cryptography;
using System.Text;

namespace Zeze.Util
{
	public class ConsistentHash<TNode>
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private SortedMap<int, TNode> circle = new();
		private Dictionary<TNode, int[]> nodes = new();

		public IReadOnlyCollection<TNode> Nodes => nodes.Keys;

		public void Add(string nodeKey, TNode node)
		{
			if (null == node)
				return;

			lock (this)
			{
				int[] vn = new int[160];
				if (!nodes.TryAdd(node, vn))
					return;

				nodeKey ??= "";
				var half = vn.Length >> 2;
				for (int i = 0; i < half; ++i)
				{
					var hash4 = MD5.Create().ComputeHash(Encoding.UTF8.GetBytes(nodeKey + "-" + i));
                    for (int j = 0; j < 4; ++j)
						vn[i * 4 + j] = BitConverter.ToInt32(hash4, j * 4);
				}
                var conflicts = circle.AddAll(vn, node);
                foreach (var conflict in conflicts)
                    logger.Warn($"hash conflict! add={nodeKey} {conflict}");
            }
        }

		public void Remove(TNode node)
		{
			lock (this)
            {
				if (false == nodes.TryGetValue(node, out var vn))
					return;

				foreach (var v in vn)
					circle.Remove(v, node);

                nodes.Remove(node);
            }
        }

		public TNode Get(int hash)
		{
            hash = FixedHash.calc_hashnr(((long)hash << 32) ^ hash);
            lock (this)
            {
                var e = circle.UpperBound(hash);
                if (e == null)
                {
                    e = circle.First;
                    if (e == null)
                        return default;
                }
                return e.Value;
            }
        }
	}
}
