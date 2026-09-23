package Zeze.Raft;

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

/**
 * follower侧InstallSnapshot接收登记表：条目即对.installing文件的所有权token。
 * 生命周期：创建（文件唯一命名{@code .installing.{index}.{seq}}，新安装永不复用
 * 旧路径，重装截断竞态结构上无对象）→ 传输 → finalizing（done置位：句柄已关、
 * 文件冻结；收尾在raft锁外await可达秒级，条目继续占位）→ 收尾finally同一性摘除。
 * <p>
 * finalizing期间对任意新块应答ResultCodeFinalizingConflict（流控：避免收尾期间的
 * 整快照重传与同边界重复提交，正确性不依赖它）。gc/cleanup对finalizing豁免空闲
 * 超时与文件清理（删其文件=删收尾提交中Files.move的源）；归属失效不豁免（其在飞
 * 收尾必在term复核处放弃且不碰文件）。
 * <p>
 * 锁序：raft→本表（既有方向），本表内部锁不嵌套raft锁。
 */
final class ReceiveSnapshotting {
	private static final Logger logger = LogManager.getLogger(ReceiveSnapshotting.class);
	private static final AtomicLong installFileSeq = new AtomicLong(); // 进程内唯一文件名序号

	private final Raft raft;
	private final Lock lock = new ReentrantLock();
	private final HashMap<Long, Entry> entries = new HashMap<>();

	// 条目=文件所有权token。finalizing置位后字段冻结（唯一写者已完成，收尾同线程读）。
	public static final class Entry {
		final Path path; // 本条目专属.installing文件（唯一名，创建时生成）
		final RandomAccessFile file;
		final long term;
		final String leaderId;
		long lastActiveTime; // 最近一次收到该安装数据块的时间（毫秒）
		// done块置位：句柄已关、文件冻结；条目由收尾的finally同一性摘除。
		boolean finalizing;
		// done时记录的应收总长度（文件冻结后的尺寸）：收尾提交前据此复核文件未被理论外
		// 路径改动（唯一命名已让重装截断无对象，此处拦磁盘异常/外部干预）。
		long expectedLength;
		// gc对idle finalizing条目的告警只发一次（慢收尾期间每20s一条会刷屏）。
		boolean idleWarned;

		public Entry(Path path, RandomAccessFile file, long term, String leaderId, long lastActiveTime) {
			this.path = path;
			this.file = file;
			this.term = term;
			this.leaderId = leaderId;
			this.lastActiveTime = lastActiveTime;
		}

		// done块收口：记录应收总长度、冻结条目、关闭句柄（三步一体，不得只置标志位）。
		void markFinalizing() throws IOException {
			expectedLength = file.length();
			finalizing = true;
			file.close();
		}
	}

	// 构造仅可存引用：本表在Raft字段初始化期构造（this逃逸），彼时raftConfig尚未
	// 赋值——dbHome/name一律延迟经raft读取，构造器中不得触碰。
	public ReceiveSnapshotting(Raft raft) {
		this.raft = raft;
	}

	// 是否有InstallSnapshot正在接收中（含finalizing收尾占位）。本地snapshot需要避开。
	public boolean isEmpty() {
		lock.lock();
		try {
			return entries.isEmpty();
		} finally {
			lock.unlock();
		}
	}

	public Entry get(long lastIncludedIndex) {
		lock.lock();
		try {
			return entries.get(lastIncludedIndex);
		} finally {
			lock.unlock();
		}
	}

	public void put(long index, Entry entry) { // 测试直接合成残留条目
		lock.lock();
		try {
			entries.put(index, entry);
		} finally {
			lock.unlock();
		}
	}

	// 同一性摘除（两参remove）：不校验会误摘同key接任的新条目。不删文件：成功路径
	// 已被Files.move消费，放弃路径在各返回点清理。
	public void removeIdentity(long index, Entry entry) {
		lock.lock();
		try {
			entries.remove(index, entry);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 接收一个InstallSnapshot数据块（follower侧）。除done块（返回finalizing条目，
	 * 由调用方收尾后应答）外，所有应答都在表锁内完成（Send为异步队列，不阻塞）。
	 */
	public Entry process(InstallSnapshot r) throws Exception {
		lock.lock();
		try {
			var index = r.Argument.getLastIncludedIndex();
			var entry = entries.get(index);
			if (entry != null && (entry.term != r.Argument.getTerm()
					|| !entry.leaderId.equals(r.Argument.getLeaderId()))) {
				// 旧归属的同边界残留：不同leader的同边界快照内容可能不同，
				// 不能续写；finalizing条目同样可弃——其在飞收尾必在term复核处放弃且不碰文件。
				logger.warn("{} discard stale-owner receive entry: LastIncludedIndex={} entryTerm={}"
								+ " entryLeader={} rpcTerm={} rpcLeader={}", raft.getName(), index,
						entry.term, entry.leaderId, r.Argument.getTerm(), r.Argument.getLeaderId());
				entries.remove(index);
				discard(entry, "ProcessInstallSnapshot");
				entry = null;
			}
			if (entry != null && entry.finalizing) {
				// 同边界上一个安装正在收尾（句柄已关、文件冻结）：应答冲突码，
				// leader中断、下个心跳重试（流控，见类注释）。
				r.SendResultCode(InstallSnapshot.ResultCodeFinalizingConflict);
				return null;
			}
			if (entry == null) {
				if (r.Argument.getOffset() != 0) {
					// 肯定是旧的被丢弃的安装，Discard And Ignore。
					r.SendResultCode(InstallSnapshot.ResultCodeOldInstall);
					return null;
				}
				// 唯一文件名：跨重启序号碰撞时文件已存在，由下方offset==0截断兜底，无害。
				var path = Paths.get(raft.getRaftConfig().getDbHome(), LogSequence.snapshotFileName
						+ ".installing." + index + "." + installFileSeq.incrementAndGet());
				entry = new Entry(path, new RandomAccessFile(path.toFile(), "rw"),
						r.Argument.getTerm(), r.Argument.getLeaderId(), System.currentTimeMillis());
				entries.put(index, entry);
			}
			entry.lastActiveTime = System.currentTimeMillis(); // 任何块活动都证明对端还活着
			var outputFileStream = entry.file;
			if (r.Argument.getOffset() == 0) {
				// offset==0无条件截断：同边界快照重生成后字节可能不同，按旧长度
				// 续传会混拼。新条目文件本为空，此截断实际作用于同条目重发块与序号碰撞残留。
				outputFileStream.setLength(0);
				outputFileStream.seek(0);
			}

			r.Result.setOffset(-1); // 默认让Leader继续传输，不用重新定位。
			long fileLength = outputFileStream.length();
			if (r.Argument.getOffset() > fileLength) {
				// 数据块超出当前已经接收到的数据。
				// 填写当前长度，让Leader从该位置开始重新传输。
				r.Result.setOffset(fileLength);
				r.SendResultCode(InstallSnapshot.ResultCodeNewOffset);
				return null;
			}

			if (r.Argument.getOffset() == fileLength) {
				// 正常的Append流程，直接写入。
				// 3. Write data into snapshot file at given offset
				r.Argument.getData().writeToFile(outputFileStream);
			} else {
				// 重叠块（leader重传已收到的前缀）：append-only不重写已落盘字节，
				// 回报当前长度让leader从该处续传（与旧版"只写超出尾部"进度等价）。
				// 协议内本分支只剩重复投递（newEnd==fileLength）；done块结尾不齐属
				// 理论外路径：按冲突应答让leader重装全新截断——不得冻结半截文件提交
				// （尺寸复核拦不住：expectedLength记录的正是done时的文件长度）。
				if (r.Argument.getDone()
						&& r.Argument.getOffset() + r.Argument.getData().size() != fileLength) {
					r.SendResultCode(InstallSnapshot.ResultCodeFinalizingConflict);
					return null;
				}
				r.Result.setOffset(fileLength);
			}

			// 4. Reply and wait for more data chunks if done is false
			if (!r.Argument.getDone()) {
				r.SendResultCode(Procedure.Success);
				return null;
			}
			// 5. Save snapshot file, discard any existing or partial snapshot with a smaller index
			// 置finalizing：所有权延长到"收尾完成"，leader对done块的超时重装由该状态
			// 挡为冲突码；条目由收尾的finally同一性摘除。
			try {
				entry.markFinalizing();
			} catch (IOException e) {
				// 读长/关闭异常不向上抛：finalizing未必置位，收尾的所有权复核会拦下并应答冲突码。
				logger.warn("ProcessInstallSnapshot markFinalizing Exception", e);
			}
			cleanupSmallerThan(entries, r.Argument.getLastIncludedIndex());
			return entry;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清理更小LastIncludedIndex的中断安装条目及其.installing文件。
	 * finalizing条目跳过：其文件正被收尾提交（Files.move的源）。必须持本表锁调用；
	 * 纯函数形态（显式操作传入的map，便于测试直接合成，与discard/tryDelete同族）。
	 * 删除失败仅告警不中断：异常传出会让更旧条目永久残留（无其他运行期清理路径），
	 * isReceivingSnapshot()恒true、本地快照停摆。残留文件不损正确性：新安装唯一命名。
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

	// 周期清理残留接收条目（onLowPrecisionTimer约20s一次）。不变量：只为
	// "当前term的当前leader"的安装保留条目，空闲超时兜底；清理后旧leader续传将收到
	// OldInstall或按新文件重传，有界自愈。
	public void gc(long now, long term, String leaderId, long idleTimeout) {
		lock.lock();
		try {
			for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				var entry = e.getValue();
				// leaderId为null/空（选举中）时无法判定归属，靠term+空闲超时判定。
				// finalizing豁免空闲超时（收尾合法地可超过派生阈值；gc动作只能二选一：
				// 摘条目复现竞态或删文件打断move），仅告警；归属失效不豁免（在飞收尾
				// 必在term复核放弃，丢弃安全）。
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
						entry.idleWarned = true; // 慢收尾期间只告警一次（gc约20s一轮）
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
		lock.lock(); // cancel不中断
		try {
			// 关句柄同时删文件：条目移出map后运行期无清理路径，
			// 不删则磁盘按快照大小泄漏（启动清理仅进程重启执行）。
			for (var it = entries.entrySet().iterator(); it.hasNext(); ) {
				var e = it.next();
				it.remove();
				discard(e.getValue(), "cancelAllReceiveSnapshotting");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 丢弃一个接收条目：关句柄+尽力删条目自己的.installing文件，失败仅告警不抛出
	 * （残留由gc/启动清理兜底）。条目的map移除由调用方完成（迭代中删除须it.remove()）。
	 */
	static void discard(Entry entry, String logTag) {
		try {
			entry.file.close();
		} catch (IOException e) {
			logger.warn("{} close Exception", logTag, e); // 文件关闭异常还是不向上抛了
		}
		tryDelete(entry.path, logTag);
	}

	// 尽力删除.installing文件：失败仅告警，残留由启动清扫兜底。
	static void tryDelete(Path path, String logTag) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			logger.warn("{} deleteIfExists Exception. path={}", logTag, path, e);
		}
	}
}
