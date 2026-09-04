package Zeze.Raft;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Raft.cleanupStaleReceiveSnapshotting 对删除失败条目的容忍（FND2-R1-1）。
 * InstallSnapshot(done) 完成分支清理更小 LastIncludedIndex 的中断安装条目时，
 * 旧实现的 Files.delete 抛 IOException（Windows 上 close 异常后句柄未释放、
 * 杀毒/备份软件短暂锁文件、磁盘 IO 错误）会直接传出清理循环：尚未处理到的
 * 更旧条目永久残留（正常运维周期内没有其他清理路径），isReceivingSnapshot()
 * 从此恒 true，LogSequence.snapshot() 恒提前返回，本地快照与日志压缩永久
 * 停摆；本次 done 的应答也发不出去。
 * 修复：Files.deleteIfExists + 逐条 try/catch，删除失败仅告警不中断清理。
 * 这里直接调用提取出的静态清理函数（不构造 Raft、不占端口、无网络），用
 * "非空目录"模拟删除必然失败（DirectoryNotEmptyException，跨平台确定，
 * 等价于第三方锁住文件）：旧代码在第一个失败条目上抛出异常，测试即失败，
 * 有区分度。
 */
@Fast
public class TestInstallSnapshotCleanupStale {

	// 删除失败不中断清理：失败条目仍被移出 map（句柄已关闭，isReceivingSnapshot
	// 可恢复），后续条目照常清理；不小于边界的条目不动。
	@Test
	public void testDeleteFailureDoesNotAbortCleanup(@TempDir Path dbHome) throws Exception {
		var map = new HashMap<Long, RandomAccessFile>();

		// 条目3：.installing.3 路径放非空目录，Files.deleteIfExists 必抛
		// DirectoryNotEmptyException（模拟杀毒/备份锁文件等删除失败，跨平台确定）。
		// RandomAccessFile 与删除目标独立：close 成功、delete 失败。
		var undeletable = Files.createDirectories(dbHome.resolve(LogSequence.snapshotFileName + ".installing.3"));
		Files.writeString(undeletable.resolve("locked.bin"), "x");
		var stale3 = new RandomAccessFile(
				dbHome.resolve(LogSequence.snapshotFileName + ".installing.9").toFile(), "rw");
		map.put(3L, stale3);

		// 条目4：正常的中断安装残留，先 close 后 delete，应被删除。
		var stale4Path = dbHome.resolve(LogSequence.snapshotFileName + ".installing.4");
		var stale4 = new RandomAccessFile(stale4Path.toFile(), "rw");
		map.put(4L, stale4);

		// 条目7：不小于 done 的 LastIncludedIndex=5，必须原样保留（可能是更新的安装）。
		var newerPath = dbHome.resolve(LogSequence.snapshotFileName + ".installing.7");
		var newer = new RandomAccessFile(newerPath.toFile(), "rw");
		map.put(7L, newer);

		Raft.cleanupStaleReceiveSnapshotting(map, dbHome.toString(), 5);

		// 失败条目也必须移出 map：残留条目会让 isReceivingSnapshot() 恒 true。
		assertEquals(1, map.size());
		assertSame(newer, map.get(7L));
		// 句柄已关闭（不再泄漏），保留的条目不动。
		assertFalse(stale3.getFD().valid());
		assertFalse(stale4.getFD().valid());
		assertTrue(newer.getFD().valid());
		// 正常条目的文件已删除；删除失败的目录保留（仅告警），不损正确性。
		assertFalse(Files.exists(stale4Path));
		assertTrue(Files.isDirectory(undeletable));
		assertTrue(Files.exists(newerPath));
		newer.close(); // 释放句柄，让@TempDir清理在Windows下也能成功。
	}

	// 外部误删后的 NoSuchFileException 由 deleteIfExists 吸收：不抛、条目照常移出。
	@Test
	public void testMissingFileIsTolerated(@TempDir Path dbHome) throws Exception {
		var map = new HashMap<Long, RandomAccessFile>();
		// 不创建 .installing.1 文件，直接放一个指向别处的句柄。
		var stale1 = new RandomAccessFile(
				dbHome.resolve(LogSequence.snapshotFileName + ".installing.9").toFile(), "rw");
		map.put(1L, stale1);

		Raft.cleanupStaleReceiveSnapshotting(map, dbHome.toString(), 2);

		assertTrue(map.isEmpty());
		assertFalse(stale1.getFD().valid());
	}
}
