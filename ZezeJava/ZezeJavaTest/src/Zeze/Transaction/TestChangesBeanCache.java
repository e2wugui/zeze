package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Util.LongHashMap;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** Public canonical-node and mutable-map behavior, independent of cache storage layout. */
@Fast
public class TestChangesBeanCache {
	private static final class CacheBean extends EmptyBean {
		private int creations;
		private Function<Changes, LogBean> factory = changes -> new LogBean(null, 0, this);

		@Override
		public LogBean createChangeLog(Changes changes) {
			++creations;
			return factory.apply(changes);
		}
	}

	@Test
	public void everyRequestedNodeRemainsCanonicalBeforeAndAfterPublicMapExposure() {
		for (int count : new int[] {0, 1, 2, 3, 8}) {
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
		var replacement = new LogBean(null, 0, first);
		exposed.put(first.objectId(), replacement);
		assertSame(replacement, changes.getOrCreateLogBean(first));
		assertEquals(1, first.creations);
		assertSame(oldSecond, exposed.remove(second.objectId()));
		var newSecond = changes.getOrCreateLogBean(second);
		assertNotSame(oldSecond, newSecond);
		assertEquals(2, second.creations);
		assertSame(newSecond, exposed.get(second.objectId()));
		var injectedBean = new CacheBean();
		var injectedNode = new LogBean(null, 0, injectedBean);
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

	@Test
	public void aFactoryCanCreateOtherNodesAndExposeTheMapWithoutLosingEntries() {
		for (int childCount : new int[] {2, 3}) {
			for (boolean exposeFirst : new boolean[] {false, true}) {
				var changes = Map2TestSupport.changes();
				var parent = new CacheBean();
				var children = new CacheBean[childCount];
				var childNodes = new LogBean[childCount];
				for (int i = 0; i < childCount; ++i)
					children[i] = new CacheBean();
				var exposed = new AtomicReference<LongHashMap<LogBean>>();
				parent.factory = duringCreation -> {
					if (exposeFirst)
						exposed.set(duringCreation.getBeans());
					for (int i = 0; i < children.length; ++i)
						childNodes[i] = duringCreation.getOrCreateLogBean(children[i]);
					if (!exposeFirst)
						exposed.set(duringCreation.getBeans());
					return new LogBean(null, 0, parent);
				};
				var parentNode = changes.getOrCreateLogBean(parent);
				assertSame(parentNode, changes.getOrCreateLogBean(parent));
				assertEquals(1, parent.creations);
				assertSame(exposed.get(), changes.getBeans());
				assertEquals(childCount + 1, changes.getBeans().size());
				assertSame(parentNode, changes.getBeans().get(parent.objectId()));
				for (int i = 0; i < children.length; ++i) {
					assertSame(childNodes[i], changes.getOrCreateLogBean(children[i]));
					assertSame(childNodes[i], exposed.get().get(children[i].objectId()));
					assertEquals(1, children[i].creations);
				}
			}
		}
	}

	@Test
	public void aCanonicalNodePublishedDuringFactoryExecutionWinsOverItsReturnedCandidate() {
		var changes = Map2TestSupport.changes();
		var bean = new CacheBean();
		var published = new LogBean(null, 0, bean);
		var returned = new LogBean(null, 0, bean);
		bean.factory = duringCreation -> {
			duringCreation.getOrCreateLogBean(new CacheBean());
			duringCreation.getOrCreateLogBean(new CacheBean());
			duringCreation.getOrCreateLogBean(new CacheBean());
			duringCreation.getBeans().put(bean.objectId(), published);
			return returned;
		};
		assertSame(published, changes.getOrCreateLogBean(bean));
		assertSame(published, changes.getOrCreateLogBean(bean));
		assertSame(published, changes.getBeans().get(bean.objectId()));
		assertEquals(4, changes.getBeans().size());
		assertEquals(1, bean.creations);
	}

	@Test
	public void aFailedFactoryDoesNotCacheItsNodeOrDiscardSuccessfulReentrantNodes() {
		var changes = Map2TestSupport.changes();
		var bean = new CacheBean();
		var firstChild = new CacheBean();
		var secondChild = new CacheBean();
		var failure = new IllegalStateException("factory failed after creating children");
		bean.factory = duringCreation -> {
			duringCreation.getOrCreateLogBean(firstChild);
			duringCreation.getOrCreateLogBean(secondChild);
			if (bean.creations == 1)
				throw failure;
			return new LogBean(null, 0, bean);
		};
		assertSame(failure, assertThrows(IllegalStateException.class, () -> changes.getOrCreateLogBean(bean)));
		var exposed = changes.getBeans();
		assertEquals(2, exposed.size());
		assertNull(exposed.get(bean.objectId()));
		assertSame(exposed.get(firstChild.objectId()), changes.getOrCreateLogBean(firstChild));
		assertSame(exposed.get(secondChild.objectId()), changes.getOrCreateLogBean(secondChild));
		var retried = changes.getOrCreateLogBean(bean);
		assertSame(retried, changes.getOrCreateLogBean(bean));
		assertSame(retried, exposed.get(bean.objectId()));
		assertSame(exposed, changes.getBeans());
		assertEquals(3, exposed.size());
		assertEquals(2, bean.creations);
		assertEquals(1, firstChild.creations);
		assertEquals(1, secondChild.creations);
	}
}
