package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import Zeze.Transaction.Collections.LogBean;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** Public canonical-node and mutable-map behavior, independent of cache storage layout. */
@Fast
public class TestChangesBeanCache {
	private static final class CacheBean extends EmptyBean {
		private int creations;

		@Override
		public LogBean createLogBean(@org.jspecify.annotations.Nullable TransactionLog finalLog) {
			++creations;
			return new LogBean(0, this);
		}
	}

	@Test
	public void everyRequestedNodeRemainsCanonicalBeforeAndAfterPublicMapExposure() {
		for (int count : new int[]{0, 1, 2, 3, 8}) {
			var changes = Map2TestSupport.changes();
			var beans = new CacheBean[count];
			var nodes = new LogBean[count];
			for (int i = 0; i < count; ++i) {
				beans[i] = new CacheBean();
				nodes[i] = changes.getOrCreateLogBean(beans[i]);
			}
			for (int i = count - 1; i >= 0; --i) {
				assertSame(nodes[i], changes.getOrCreateLogBean(beans[i]));
				assertEquals(1, beans[i].creations);
			}
			var exposed = changes.getBeans();
			assertEquals(count, exposed.size());
			assertSame(exposed, changes.getBeans());
			for (int i = 0; i < count; ++i) {
				assertSame(nodes[i], exposed.get(beans[i].objectId()));
				assertSame(nodes[i], changes.getOrCreateLogBean(beans[i]));
				assertEquals(1, beans[i].creations);
			}
		}
	}

	@Test
	public void exposingAnEmptyMapKeepsTheSamePublicMapForAllLaterNodes() {
		var changes = Map2TestSupport.changes();
		var exposed = changes.getBeans();
		assertEquals(0, exposed.size());
		var beans = new CacheBean[8];
		var nodes = new LogBean[8];
		for (int i = 0; i < beans.length; ++i) {
			beans[i] = new CacheBean();
			nodes[i] = changes.getOrCreateLogBean(beans[i]);
			assertSame(exposed, changes.getBeans());
			assertEquals(i + 1, exposed.size());
			for (int prior = 0; prior <= i; ++prior)
				assertSame(nodes[prior], exposed.get(beans[prior].objectId()));
		}
	}

	@Test
	public void externalPutRemoveAndClearRemainAuthoritative() {
		var changes = Map2TestSupport.changes();
		var first = new CacheBean();
		var second = new CacheBean();
		changes.getOrCreateLogBean(first);
		var oldSecond = changes.getOrCreateLogBean(second);
		var exposed = changes.getBeans();
		var replacement = new LogBean(0, first);
		exposed.put(first.objectId(), replacement);
		assertSame(replacement, changes.getOrCreateLogBean(first));
		assertEquals(1, first.creations);
		assertSame(oldSecond, exposed.remove(second.objectId()));
		var newSecond = changes.getOrCreateLogBean(second);
		assertNotSame(oldSecond, newSecond);
		assertEquals(2, second.creations);
		assertSame(newSecond, exposed.get(second.objectId()));
		var injectedBean = new CacheBean();
		var injectedNode = new LogBean(0, injectedBean);
		exposed.put(injectedBean.objectId(), injectedNode);
		assertSame(injectedNode, changes.getOrCreateLogBean(injectedBean));
		assertEquals(0, injectedBean.creations);
		exposed.clear();
		assertEquals(0, changes.getBeans().size());
		assertNotSame(replacement, changes.getOrCreateLogBean(first));
		assertNotSame(newSecond, changes.getOrCreateLogBean(second));
		assertEquals(2, first.creations);
		assertEquals(3, second.creations);
		assertEquals(2, exposed.size());
		assertNull(exposed.get(injectedBean.objectId()));
		assertSame(exposed, changes.getBeans());
	}
}
