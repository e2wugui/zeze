package Zeze.Util;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ReflectHandleCache 契约测试：
 * 缓存复用、私有无参构造支持、类型校验防缓存污染、反射失败异常传播与并发单次实例化。
 */
@Fast
public class TestReflectHandleCache {
	interface SampleHandle {
		String tag();
	}

	public static final class PublicHandle implements SampleHandle {
		@Override
		public String tag() {
			return "public";
		}
	}

	private static final class PrivateCtorHandle implements SampleHandle {
		private PrivateCtorHandle() {
		}

		@Override
		public String tag() {
			return "private";
		}
	}

	public static final class NoDefaultCtorHandle implements SampleHandle {
		public NoDefaultCtorHandle(int unused) {
		}

		@Override
		public String tag() {
			return "no-default";
		}
	}

	public static final class UnrelatedType {
		static final AtomicInteger ctorCalls = new AtomicInteger();

		public UnrelatedType() {
			ctorCalls.incrementAndGet();
		}
	}

	public static final class CountingHandle implements SampleHandle {
		static final AtomicInteger ctorCalls = new AtomicInteger();

		public CountingHandle() {
			ctorCalls.incrementAndGet();
		}

		@Override
		public String tag() {
			return "counting";
		}
	}

	@Test
	public void instantiatesAndCachesPublicAndPrivateHandles() {
		var cache = new ReflectHandleCache<>(SampleHandle.class);

		var h1 = cache.findHandle(PublicHandle.class.getName());
		var h2 = cache.findHandle(PublicHandle.class.getName());
		Assertions.assertSame(h1, h2);
		Assertions.assertEquals("public", h1.tag());

		var p1 = cache.findHandle(PrivateCtorHandle.class.getName());
		var p2 = cache.findHandle(PrivateCtorHandle.class.getName());
		Assertions.assertSame(p1, p2);
		Assertions.assertEquals("private", p1.tag());
	}

	@Test
	public void rejectsIncompatibleTypeWithoutInstantiatingOrCaching() {
		UnrelatedType.ctorCalls.set(0);
		var cache = new ReflectHandleCache<>(SampleHandle.class);

		Assertions.assertThrows(ClassCastException.class,
				() -> cache.findHandle(UnrelatedType.class.getName()));
		Assertions.assertEquals(0, UnrelatedType.ctorCalls.get(),
				"类型不匹配时必须在实例化前 fail-fast，不得触发无关类构造函数");
		Assertions.assertThrows(ClassCastException.class,
				() -> cache.findHandle(UnrelatedType.class.getName()));
		Assertions.assertEquals(0, UnrelatedType.ctorCalls.get());
	}

	@Test
	public void propagatesReflectiveFailures() {
		var cache = new ReflectHandleCache<>(SampleHandle.class);

		Assertions.assertThrows(ClassNotFoundException.class,
				() -> cache.findHandle("Zeze.Util.NonExistentHandleClassForTest"));
		Assertions.assertThrows(NoSuchMethodException.class,
				() -> cache.findHandle(NoDefaultCtorHandle.class.getName()));
	}

	@Test
	public void concurrentFirstLookupInstantiatesOnce() throws Exception {
		CountingHandle.ctorCalls.set(0);
		var cache = new ReflectHandleCache<>(SampleHandle.class);
		int threads = 8;
		var start = new CountDownLatch(1);
		var pool = Executors.newFixedThreadPool(threads);
		try {
			var futures = new ArrayList<Future<SampleHandle>>(threads);
			for (int i = 0; i < threads; i++) {
				futures.add(pool.submit(() -> {
					start.await();
					return cache.findHandle(CountingHandle.class.getName());
				}));
			}
			start.countDown();
			SampleHandle first = null;
			for (var f : futures) {
				var h = f.get();
				if (first == null)
					first = h;
				else
					Assertions.assertSame(first, h);
			}
			Assertions.assertEquals(1, CountingHandle.ctorCalls.get(),
					"并发首次查找同一句柄类应只实例化一次");
		} finally {
			pool.shutdownNow();
		}
	}
}
