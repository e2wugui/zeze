package Zeze.Raft;

import java.util.Objects;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * follower 侧 InstallSnapshot 接收登记表：条目作为 {@code .installing.{index}.{seq}} 文件的所有权凭证。
 * done 块后进入 finalizing（文件冻结并关闭句柄），占位至收尾在 finally 中按同一性摘除；
 * finalizing 期间对同边界新块返回 ResultCodeFinalizingConflict，且 gc 豁免空闲超时（归属失效不豁免）。
 * 锁序：raft -> 本表，本表锁内不嵌套 raft 锁。
 */
final class ReceiveSnapshotting {
	private static final Logger logger = LogManager.getLogger(ReceiveSnapshotting.class);
	private static final AtomicLong installFileSeq = new AtomicLong(); // 进程内唯一文件名序号

	private final Raft raft;
	private final Lock lock = new ReentrantLock();
	private final HashMap<Long, Entry> entries = new HashMap<>();

	// 条目作为文件所有权凭证；finalizing 置位后字段冻结。
	public static final class Entry {
		final Path path; // 本条目专属 .installing 文件
		final RandomAccessFile file;
		final long term;
		final String leaderId;
		long lastActiveTime; // 最近收到数据块的时间（毫秒）
		// done 块置位：句柄已关闭且文件冻结，由收尾在 finally 中按同一性摘除。
		boolean finalizing;
		// done 块时记录的文件长度，收尾提交前据此校验文件完整性。
		long expectedLength;
		// 防止慢收尾期间 gc 重复刷屏告警。
		boolean idleWarned;

		public Entry(Path path, RandomAccessFile file, long term, String leaderId, long lastActiveTime) {
			this.path = path;
			this.file = file;
			this.term = term;
			this.leaderId = leaderId;
			this.lastActiveTime = lastActiveTime;
		}

		// done 块收口：记录长度、标记冻结并关闭文件句柄。
		void markFinalizing() throws IOException {
			expectedLength = file.length();
			finalizing = true;
			file.close();
		}
	}

	// 在 Raft 构造期初始化，dbHome/name 须延迟经 raft 读取。
	public ReceiveSnapshotting(Raft raft) {
		this.raft = raft;
	}

	// 是否有正在接收或收尾中的快照，本地 snapshot 需避开。
	public boolean isEmpty() {
		lock.lock();
		try {
			return entries.isEmpty();
		} finally {
			lock.unlock();
		}
	}

	public @Nullable Entry get(long lastIncludedIndex) {
		lock.lock();
		try {
			return entries.get(lastIncludedIndex);
		} finally {
			lock.unlock();
		}
	}

	public void put(long index, Entry entry) {
		lock.lock();
		try {
			entries.put(index, entry);
		} finally {
			lock.unlock();
		}
	}

	// 按同一性摘除条目，不误删同 index 新条目；文件由提交移动或放弃路径自行清理。
	public void removeIdentity(long index, Entry entry) {
		lock.lock();
		try {
			entries.remove(index, entry);
		} finally {
			lock.unlock();
		}
	}

	/** 提交或应用前缀超过接收边界时作废接收：与写块同持表锁，关文件与摘登记原子互斥。 */
	void discard(long index) {
		lock.lock();
		try {
			var entry = entries.remove(index);
			if (entry != null)
				discard(entry, "stale snapshot");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 接收单个 InstallSnapshot 数据块；done 块返回 finalizing 条目交由调用方收尾，其余块在表锁内直接应答。
	 * 调用方须持有 Raft 锁贯穿归属校验与本次调用。
	 */
	public @Nullable Entry process(InstallSnapshot r) throws Exception {
		lock.lock();
		try {
			var index = r.Argument.getLastIncludedIndex();
			var entry = entries.get(index);
			if (entry != null && (entry.term != r.Argument.getTerm()
					|| !entry.leaderId.equals(r.Argument.getLeaderId()))) {
				// 旧归属的同边界残留不可续写，即使处于 finalizing 也直接作废（其收尾会在 term 复核时放弃）。
				logger.warn("{} discard stale-owner receive entry: LastIncludedIndex={} entryTerm={}"
								+ " entryLeader={} rpcTerm={} rpcLeader={}", raft.getName(), index,
						entry.term, entry.leaderId, r.Argument.getTerm(), r.Argument.getLeaderId());
				entries.remove(index);
				discard(entry, "ProcessInstallSnapshot");
				entry = null;
			}
			if (entry != null && entry.finalizing) {
				// 同边界上一个安装正在收尾，返回冲突码让 leader 下次心跳重试。
				r.SendResultCode(InstallSnapshot.ResultCodeFinalizingConflict);
				return null;
			}
			if (entry == null) {
				if (r.Argument.getOffset() != 0) {
					// 旧的已丢弃安装，忽略。
					r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
					return null;
				}
				// 唯一文件名；若跨重启序号碰撞，下方 offset==0 会截断重写。
				var path = Paths.get(Objects.requireNonNull(raft.getRaftConfig().getDbHome()), LogSequence.snapshotFileName
						+ ".installing." + index + "." + installFileSeq.incrementAndGet());
				entry = new Entry(path, new RandomAccessFile(path.toFile(), "rw"),
						r.Argument.getTerm(), Objects.requireNonNull(r.Argument.getLeaderId()), System.currentTimeMillis());
				entries.put(index, entry);
			}
			entry.lastActiveTime = System.currentTimeMillis();
			var outputFileStream = entry.file;
			if (r.Argument.getOffset() == 0) {
				// offset==0 无条件截断，避免同边界重传或序号碰撞残留导致内容混拼。
				outputFileStream.setLength(0);
				outputFileStream.seek(0);
			}

			r.Result.setOffset(-1); // -1 表示按顺序续传
			long fileLength = outputFileStream.length();
			if (r.Argument.getOffset() > fileLength) {
				// 偏移越界，回报当前长度让 leader 重定位。
				r.Result.setOffset(fileLength);
				r.SendResultCode(InstallSnapshot.ResultCodeNewOffset);
				return null;
			}

			if (r.Argument.getOffset() == fileLength) {
				// 顺序追加写入数据块
				//noinspection DataFlowIssue
				r.Argument.getData().writeToFile(outputFileStream);
			} else {
				// 重叠块按 append-only 忽略已落盘前缀并回报当前长度；
				// done 块末尾与当前长度不一致则按冲突拒绝，避免提交半截文件。
				//noinspection DataFlowIssue
				if (r.Argument.getDone()
						&& r.Argument.getOffset() + r.Argument.getData().size() != fileLength) {
					r.SendResultCode(InstallSnapshot.ResultCodeFinalizingConflict);
					return null;
				}
				r.Result.setOffset(fileLength);
			}

			if (!r.Argument.getDone()) {
				r.SendResultCode(Procedure.Success);
				return null;
			}
			// 冻结文件进入 finalizing，延长占位至收尾完成。
			try {
				entry.markFinalizing();
			} catch (IOException e) {
				// 异常时不向上抛，交由收尾的所有权与长度复核拦下。
				logger.warn("ProcessInstallSnapshot markFinalizing Exception", e);
			}
			cleanupSmallerThan(entries, r.Argument.getLastIncludedIndex());
			return entry;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清理更小 LastIncludedIndex 的非 finalizing 条目及其临时文件（须持本表锁调用）。
	 * 删除失败仅告警不中断，避免残留条目阻塞后续本地快照。
	 */
	static void cleanupSmallerThan(HashMap<Long, Entry> entries, long lastIncludedIndex) {
		for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			if (e.getKey() < lastIncludedIndex && !e.getValue().finalizing) {
				it.remove();
				discard(e.getValue(), "cleanupSmallerThan");
			}
		}
	}

	// 周期清理过期的接收条目（调用方持 Raft 锁）：仅保留当前 term/leader 的活跃条目。
	public void gc(long now, long term, @Nullable String leaderId, long idleTimeout) {
		lock.lock();
		try {
			for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var entry = e.getValue();
				// finalizing 条目豁免空闲超时（防止打断收尾 move）仅告警一次；归属失效则直接清理。
				var staleOwner = entry.term != term
						|| (leaderId != null && !leaderId.isEmpty() && !entry.leaderId.equals(leaderId));
				var idle = now - entry.lastActiveTime > idleTimeout;
				if (staleOwner || (idle && !entry.finalizing)) {
					it.remove();
					discard(entry, "gcReceiveSnapshotting");
					logger.warn("{} gcReceiveSnapshotting: removed stale receive entry. LastIncludedIndex={}"
									+ " entryTerm={} entryLeader={} idle={}ms currentTerm={} currentLeader={}",
							raft.getName(), e.getKey(), entry.term, entry.leaderId,
							now - entry.lastActiveTime, term, leaderId);
				} else if (idle) {
					if (!entry.idleWarned) {
						entry.idleWarned = true;
						logger.warn("{} gcReceiveSnapshotting: finalizing entry still in finalize"
										+ " (endReceiveInstallSnapshot in flight?), keep it. LastIncludedIndex={}"
										+ " finalizeElapsed={}ms",
								raft.getName(), e.getKey(), now - entry.lastActiveTime);
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void cancelAll() {
		lock.lock(); // cancel 不中断
		try {
			// 关句柄并删除临时文件，防止条目摘除后磁盘泄漏。
			for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				it.remove();
				discard(e.getValue(), "cancelAllReceiveSnapshotting");
			}
		} finally {
			lock.unlock();
		}
	}

	/** 关闭条目句柄并尽力删除临时文件（不抛出异常，map 移除由调用方负责）。 */
	static void discard(Entry entry, String logTag) {
		try {
			entry.file.close();
		} catch (IOException e) {
			logger.warn("{} close Exception", logTag, e);
		}
		tryDelete(entry.path, logTag);
	}

	// 尽力删除临时文件，失败仅告警。
	static void tryDelete(Path path, String logTag) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			logger.warn("{} deleteIfExists Exception. path={}", logTag, path, e);
		}
	}
}
