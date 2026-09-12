package UnitTest.Zeze.Util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Util.ConcurrentHashMapOrdered;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-14：clear() 三步（queue.clear/map.clear/size.set(0)）非原子，与并发 put 交错时
 * increment 被 set(0) 覆盖——map 有条目而 size==0/isEmpty，后续 remove 再减成负数，
 * 永久错乱。修复：clear 整体替换 state（queue/map/size 三组件单一引用），旧 state 上
 * 迟到增量落在废弃对象上；静止后 size() 与可见条目数必须一致。
 * 竞态窗口窄（两条指令），红为压测概率形态：8线程put持续满载×500ms×clear满频。
 */
@Fast
public class TestConcurrentHashMapOrderedClear {

	@Test
	public void testClearPutRaceSizeConsistency() throws Exception {
		var m = new ConcurrentHashMapOrdered<Integer, Integer>();
		var keyGen = new AtomicInteger();
		var running = new AtomicBoolean(true);
		var putters = new Thread[8];
		for (int t = 0; t < putters.length; t++)
			putters[t] = new Thread(() -> {
				while (running.get())
					m.put(keyGen.incrementAndGet(), 1);
			});
		var clearer = new Thread(() -> {
			while (running.get())
				m.clear();
		});
		for (var p : putters)
			p.start();
		clearer.start();
		Thread.sleep(500);
		running.set(false);
		for (var p : putters)
			p.join();
		clearer.join();

		// 静止后不变量：size() == 可见条目数，且非负
		var visible = new AtomicInteger();
		m.foreach((k, v) -> visible.incrementAndGet());
		Assertions.assertTrue(m.size() >= 0, "size不得为负: " + m.size());
		Assertions.assertEquals(visible.get(), m.size(),
				"静止后 size 必须等于可见条目数（clear与put竞态不得留下永久错乱）");
	}
}
