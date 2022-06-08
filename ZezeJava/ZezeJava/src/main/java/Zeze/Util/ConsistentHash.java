package Zeze.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsistentHash<TNode> {
	private static final Logger logger = LogManager.getLogger(ConsistentHash.class);

	private final int numberOfReplicas;
	private final TreeMap<Integer, TNode> circle = new TreeMap<>();
	private final HashSet<TNode> nodes = new HashSet<>();
	private final Set<TNode> nodesView = Collections.unmodifiableSet(nodes);

	public ConsistentHash() {
		this(128);
	}

	public ConsistentHash(int numberOfReplicas) {
		if (numberOfReplicas <= 0)
			throw new IllegalArgumentException();
		this.numberOfReplicas = numberOfReplicas;
	}

	public Set<TNode> getNodes() {
		return nodesView;
	}

	public synchronized void add(String nodeKey, TNode node) {
		if (null == node)
			return;

		if (!nodes.add(node))
			return;

		if (nodeKey == null)
			nodeKey = "";

		for (int i = 0; i < numberOfReplicas; ++i) {
			var hash = Zeze.Transaction.Bean.Hash32(nodeKey + "#" + i);
			var conflict = circle.putIfAbsent(hash, node);
			if (null != conflict) {
				logger.warn("hash conflict! add=" + nodeKey + " i=" + i + " exist=" + conflict);
			}
		}
	}

	public synchronized void remove(String nodeKey, TNode node) {
		if (null == node)
			return;

		if (!nodes.remove(node))
			return;

		if (nodeKey == null)
			nodeKey = "";

		for (int i = 0; i < numberOfReplicas; ++i) {
			var hash = Zeze.Transaction.Bean.Hash32(nodeKey + "#" + i);
			circle.remove(hash, node);
		}
	}

	public synchronized TNode get(int hash) {
		var e = circle.ceilingEntry(hash);
		if (e == null) {
			e = circle.firstEntry();
			if (e == null)
				return null;
		}
		return e.getValue();
	}
}
