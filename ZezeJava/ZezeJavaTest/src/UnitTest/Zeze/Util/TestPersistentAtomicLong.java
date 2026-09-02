package UnitTest.Zeze.Util;
import harness.Fast;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import Zeze.Util.PersistentAtomicLong;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@SuppressWarnings("CallToPrintStackTrace")
@Fast
public class TestPersistentAtomicLong {
	@Test
	public void testConcurrent() {
		Task.tryInitThreadPool();

		var p1 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var p2 = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong");
		var jobs = new ArrayList<Future<?>>();
		jobs.add(TaskSpec.ofAction(() -> Alloc(p1)).name("Alloc1").submitNow());
		jobs.add(TaskSpec.ofAction(() -> Alloc(p2)).name("Alloc2").submitNow());
		Task.waitAll(jobs);
	}

	final ConcurrentHashMap<Long, Long> allocs = new ConcurrentHashMap<>();

	private void Alloc(PersistentAtomicLong p) {
		try {
			for (int i = 0; i < 1000; ++i) {
				var n = p.next();
				Assertions.assertNull(allocs.put(n, n));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Assertions.fail();
		}
	}

	@Test
	public void testNextCountNotExceedAllocatedEnd() throws Exception {
		Task.tryInitThreadPool();

		var pal = PersistentAtomicLong.getOrAdd("TestPersistentAtomicLong.U3");
		// 反射构造"剩余预算不足count"的状态：currentId=100, allocatedEnd=102（剩余2），请求next(5)。
		var fCurrent = PersistentAtomicLong.class.getDeclaredField("currentId");
		fCurrent.setAccessible(true);
		((java.util.concurrent.atomic.AtomicLong)fCurrent.get(pal)).set(100);
		var fEnd = PersistentAtomicLong.class.getDeclaredField("allocatedEnd");
		fEnd.setAccessible(true);
		fEnd.setLong(pal, 102);

		var returned = pal.next(5);

		// 语义：count个号的整块[current+1, current+count]必须落在水位allocatedEnd之内。
		// 越过水位的号在重启后被重复发放（重启时currentId重置为文件水位），
		// 多进程共享同一pal文件时直接与其他进程的区间冲突。
		var endAfter = fEnd.getLong(pal);
		Assertions.assertEquals(105, returned);
		Assertions.assertTrue(returned <= endAfter,
				"next(count)不得越过allocatedEnd发号: returned=" + returned + ", allocatedEnd=" + endAfter);
		Assertions.assertEquals(105, ((java.util.concurrent.atomic.AtomicLong)fCurrent.get(pal)).get());
	}

	@Test
	public void testOldFormatMigrateToFixedWidth() throws Exception {
		var name = "TestPersistentAtomicLong.OldFmt";
		var path = Path.of(name + ".zeze.pal");
		Files.deleteIfExists(path);
		Files.writeString(path, "12345"); // 旧格式：变长数字，无补齐

		var pal = PersistentAtomicLong.getOrAdd(name);
		var n = pal.next();
		Assertions.assertEquals(12346, n, "旧水位12345之上继续发号，不得重发");

		var bytes = Files.readAllBytes(path);
		Assertions.assertEquals(20, bytes.length, "迁移后必须定宽20字节，之后写入不再改变文件长度");
		var watermark = Long.parseLong(new String(bytes).trim());
		Assertions.assertTrue(watermark >= 12345 + 16, "定宽内容必须>=旧值+最小分配量: " + watermark);
	}

	@Test
	public void testReadCompatStates() throws Exception {
		// 旧格式（变长数字）
		Assertions.assertEquals(12345, watermarkOf("12345"));
		// 新格式（右对齐空格补齐到20字节）
		Assertions.assertEquals(12345, watermarkOf("               12345"));
		// 迁移中间态：旧值+任意截断点的已追加空格，解析值必须仍是完整旧值
		Assertions.assertEquals(12345, watermarkOf("12345   "));
		Assertions.assertEquals(12345, watermarkOf("12345" + " ".repeat(15)));
		// 新文件定宽写未完成的纯空格前缀 / 空文件，解析为0
		Assertions.assertEquals(0, watermarkOf("     "));
		Assertions.assertEquals(0, watermarkOf(""));
	}

	private static int readSeq = 0;

	/**
	 * 写入给定文件内容，构造PAL读一次，返回解析出的水位（watermark+1 == first next()）。
	 * 模拟重启后各种磁盘状态下的读取语义。
	 */
	private long watermarkOf(String content) throws Exception {
		var name = "TestPersistentAtomicLong.R" + readSeq++;
		var path = Path.of(name + ".zeze.pal");
		Files.deleteIfExists(path);
		Files.writeString(path, content);
		return PersistentAtomicLong.getOrAdd(name).next() - 1;
	}
}
