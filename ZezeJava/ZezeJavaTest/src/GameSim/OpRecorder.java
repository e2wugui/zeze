package GameSim;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 每操作类型延迟/计数采集：玩家线程写自己的缓冲（线程封闭无锁），窗口翻转轮换缓冲，
 * 收割时合并排序出分位。延迟单位微秒（32 位上限 ~70 分钟，远超任何单事务耗时）。
 *
 * <p>窗口协议：world.windowId 为当前测量窗口序号（0=非测量段，统计静默）。玩家在 record()
 * 时发现窗口翻转即轮换自己的缓冲；harness 在窗口结束后等一个宽限期再收割旧窗口缓冲
 * （跨界的少数长尾样本落到完成侧窗口，计数用 LongAdder 全量精确不受影响）。
 */
public final class OpRecorder {
	public static final int OP_COUNT = OpType.values().length;

	/** 事务类型=玩家可见操作类型（rank/exp 合并在 loot 事务内，不单列） */
	public enum OpType {
		login, logout, heartbeat, hbScan, loot, usePotion, equipChange, sellShop,
		trade, mailSend, mailClaim, mailFanout, friendQuery
	}

	// per-线程 per-窗口槽 per-op 缓冲（窗口奇偶双缓冲）：[slot][op] -> 样本数组
	private final long[][][] samples = new long[2][OP_COUNT][];
	private final int[][] lens = new int[2][OP_COUNT];
	private volatile int myWindow;

	// 全程计数（按窗口快照差值取每窗口速率；errors 记事务返回非 0）
	final AtomicLong[] counts = new AtomicLong[OP_COUNT];
	final AtomicLong[] errors = new AtomicLong[OP_COUNT];

	private static OpRecorder[] all;

	/** players 个玩家槽 + 1 个系统槽（hbScan/mailFanout 等非玩家线程操作） */
	public static void init(int players) {
		all = new OpRecorder[players + 1];
	}

	public static OpRecorder create(int playerIndex) {
		var r = new OpRecorder();
		all[playerIndex] = r;
		return r;
	}

	public static OpRecorder createSystem() {
		var r = new OpRecorder();
		all[all.length - 1] = r;
		return r;
	}

	private OpRecorder() {
		for (int op = 0; op < OP_COUNT; op++) {
			counts[op] = new AtomicLong();
			errors[op] = new AtomicLong();
		}
	}

	/** 记录一次操作：windowId=world 当前窗口（0=非测量段，只计数不计延迟） */
	public void record(OpType op, int windowId, long startNanos) {
		counts[op.ordinal()].incrementAndGet();
		if (windowId <= 0)
			return;
		int slot = windowId & 1;
		int opIdx = op.ordinal();
		if (myWindow != windowId) { // 窗口翻转：换槽（旧槽由 harness 收割后清零）
			myWindow = windowId;
		}
		long us = (System.nanoTime() - startNanos) / 1000;
		long[] buf = samples[slot][opIdx];
		if (buf == null) {
			buf = new long[256];
			samples[slot][opIdx] = buf;
		}
		int len = lens[slot][opIdx]++; // 仅本线程写本对象，线程封闭
		if (len >= buf.length) {
			samples[slot][opIdx] = buf = Arrays.copyOf(buf, buf.length * 2);
		}
		buf[len] = us;
	}

	public void error(OpType op) {
		errors[op.ordinal()].incrementAndGet();
	}

	public static final class OpStats {
		public final long count;
		public final double mean, p50, p95, p99, max; // 微秒

		OpStats(long[] xs) {
			count = xs.length;
			if (count == 0) {
				mean = p50 = p95 = p99 = max = 0;
				return;
			}
			Arrays.sort(xs);
			double sum = 0;
			for (long x : xs)
				sum += x;
			mean = sum / count;
			p50 = xs[(int)(count * 50 / 100)];
			p95 = xs[Math.min((int)(count * 95 / 100), (int)count - 1)];
			p99 = xs[Math.min((int)(count * 99 / 100), (int)count - 1)];
			max = xs[(int)count - 1];
		}
	}

	/** 收割指定窗口（slot=windowId&1）的所有 op 样本；收割后该槽清零复用 */
	public static OpStats[] harvest(int windowId) {
		int slot = windowId & 1;
		var stats = new OpStats[OP_COUNT];
		for (int op = 0; op < OP_COUNT; op++) {
			int total = 0;
			long[] merged = new long[0];
			for (OpRecorder r : all) {
				long[] buf = r.samples[slot][op];
				int len = r.lens[slot][op];
				if (buf == null || len == 0)
					continue;
				if (total + len > merged.length)
					merged = Arrays.copyOf(merged, Math.max(merged.length * 2, total + len));
				System.arraycopy(buf, 0, merged, total, len);
				total += len;
			}
			stats[op] = new OpStats(Arrays.copyOf(merged, total));
		}
		for (OpRecorder r : all)
			Arrays.fill(r.lens[slot], 0);
		return stats;
	}

	/** 全程计数快照（每窗口差值即窗口内次数） */
	public static long[] countsSnapshot() {
		var out = new long[OP_COUNT];
		for (int op = 0; op < OP_COUNT; op++) {
			long sum = 0;
			for (OpRecorder r : all)
				sum += r.counts[op].get();
			out[op] = sum;
		}
		return out;
	}

	public static long[] errorTotals() {
		var out = new long[OP_COUNT];
		for (int op = 0; op < OP_COUNT; op++) {
			long sum = 0;
			for (OpRecorder r : all)
				sum += r.errors[op].get();
			out[op] = sum;
		}
		return out;
	}
}
