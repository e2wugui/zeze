package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * put在compute内先入队、compute返回才发布到map：并发遍历清理不得把这个
 * "未发布"窗口里的键当成"已删除"永久摘出队列——否则map有条目而foreach/iterator
 * 永久漏项（get/size正常，问题极隐蔽）。清理只允许摘除带死亡标记的条目，
 * 未发布条目保持在线。另钉删除重插的代际身份：旧代节点回收不得误杀新代。
 */
@Fast
public class TestConcurrentHashMapOrderedPublication {

	/** add后挂起（制造compute内入队完成、map尚未发布的窗口）。 */
	@SuppressWarnings("rawtypes")
	static class GateQueue extends ConcurrentLinkedQueue {
		final CountDownLatch added = new CountDownLatch(1);
		final CountDownLatch resume = new CountDownLatch(1);

		@Override
		public boolean add(Object e) {
			var r = super.add(e);
			added.countDown();
			try {
				if (!resume.await(10, TimeUnit.SECONDS))
					throw new AssertionError("gate timeout");
			} catch (InterruptedException ex) {
				throw new AssertionError(ex);
			}
			return r;
		}
	}

	@SuppressWarnings("rawtypes")
	private static GateQueue installGate(ConcurrentHashMapOrdered<String, String> map) throws Exception {
		var stateField = ConcurrentHashMapOrdered.class.getDeclaredField("state");
		stateField.setAccessible(true);
		var state = ((AtomicReference<?>)stateField.get(map)).get();
		var queueField = state.getClass().getDeclaredField("queue");
		queueField.setAccessible(true);
		var gate = new GateQueue();
		queueField.set(state, gate);
		return gate;
	}

	private interface PutOp {
		void put(ConcurrentHashMapOrdered<String, String> map, String key, String value);
	}

	/** 发布窗口期间的并发遍历不得永久摘除该键。 */
	private static void publicationWindowSurvivesConcurrentSweep(PutOp op) throws Exception {
		var map = new ConcurrentHashMapOrdered<String, String>();
		var gate = installGate(map);
		var worker = new Thread(() -> op.put(map, "key", "value"), "ordered-put");
		worker.start();
		Assertions.assertTrue(gate.added.await(10, TimeUnit.SECONDS), "put必须已入队（窗口已打开）");
		map.foreach((k, v) -> {
		}); // 窗口内并发清理
		gate.resume.countDown();
		worker.join(10_000);

		var count = new int[1];
		map.foreach((k, v) -> count[0]++);
		Assertions.assertEquals("value", map.get("key"));
		Assertions.assertEquals(1, map.size());
		Assertions.assertEquals(1, count[0], "发布完成后遍历必须看到该键（不得在窗口内被永久摘除）");
		Assertions.assertTrue(map.iterator().hasNext(), "iterator同样不得漏项");
		Assertions.assertEquals("value", map.iterator().next());
	}

	@Test
	public void putPublicationWindowSurvivesConcurrentSweep() throws Exception {
		publicationWindowSurvivesConcurrentSweep(ConcurrentHashMapOrdered::put);
	}

	@Test
	public void putIfAbsentPublicationWindowSurvivesConcurrentSweep() throws Exception {
		publicationWindowSurvivesConcurrentSweep(ConcurrentHashMapOrdered::putIfAbsent);
	}

	/** 删除重插产生新代条目：旧代节点按自身身份回收，新代恰发射一次、不重复不丢失。 */
	@Test
	public void removeAndReinsertKeepsSingleGenerationVisible() {
		var map = new ConcurrentHashMapOrdered<String, String>();
		map.put("k", "v1");
		Assertions.assertEquals("v1", map.remove("k"));
		map.put("k", "v2");

		var seen = new ArrayList<String>();
		map.foreach((k, v) -> seen.add(k + "=" + v));
		Assertions.assertEquals(List.of("k=v2"), seen, "新代恰一次，旧代回收");

		seen.clear();
		map.foreach((k, v) -> seen.add(k + "=" + v));
		Assertions.assertEquals(List.of("k=v2"), seen, "节点回收后遍历稳定");

		var it = map.iterator();
		Assertions.assertTrue(it.hasNext());
		Assertions.assertEquals("v2", it.next());
		Assertions.assertEquals("k", it.key());
		Assertions.assertFalse(it.hasNext());
		Assertions.assertEquals(1, map.size());
	}

	/** 重写后的基本契约回归。 */
	@Test
	public void basicContract() {
		var map = new ConcurrentHashMapOrdered<String, String>();
		Assertions.assertNull(map.put("k1", "a"));
		Assertions.assertNull(map.put("k2", "b"));
		Assertions.assertEquals("a", map.put("k1", "A"));
		Assertions.assertEquals("A", map.get("k1"));

		Assertions.assertEquals("A", map.replace("k1", "AA"));
		Assertions.assertEquals("AA", map.get("k1"));
		Assertions.assertTrue(map.replace("k1", "AA", "B"));
		Assertions.assertFalse(map.replace("k1", "X", "Y"));
		Assertions.assertEquals("B", map.get("k1"));

		Assertions.assertTrue(map.remove("k2", "b"));
		Assertions.assertFalse(map.remove("k2", "other"));

		Assertions.assertEquals("B", map.putIfAbsent("k1", "Z"));
		Assertions.assertNull(map.putIfAbsent("k3", "C"));
		Assertions.assertEquals("C", map.get("k3"));
		Assertions.assertEquals("C", map.getOrDefault("k3", "D"));
		Assertions.assertEquals("D", map.getOrDefault("no", "D"));

		Assertions.assertTrue(map.containsKey("k1"));
		Assertions.assertFalse(map.containsKey("k2"));
		Assertions.assertTrue(map.containsValue("B"));
		Assertions.assertFalse(map.containsValue("a"));
		Assertions.assertEquals(2, map.size());
		Assertions.assertEquals("{k1=B, k3=C}", map.toString());

		var order = new ArrayList<String>();
		map.foreach((k, v) -> order.add(k));
		Assertions.assertEquals(List.of("k1", "k3"), order, "保持首次加入顺序");

		map.clear();
		Assertions.assertTrue(map.isEmpty());
		Assertions.assertEquals(0, map.size());
		Assertions.assertNull(map.get("k1"));
	}
}
