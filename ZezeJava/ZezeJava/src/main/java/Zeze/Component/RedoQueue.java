package Zeze.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import Zeze.Builtin.RedoQueue.BQueueTask;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Builtin.RedoQueue.RunTask;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Services.HandshakeClient;
import Zeze.Transaction.Procedure;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.RocksDBException;

/**
 * 连接：
 * 1.每个队列一个连接服务。
 * 2.可以从可用的zeze-server中选择部分，配置到zeze.xml中。
 * 3.【可选】使用ServiceManager动态发现zeze-server。感觉没有必要。
 */
public class RedoQueue extends HandshakeClient {
	private static final org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(RedoQueue.class);
	private RocksDatabase db;
	private RocksDatabase.Table tableLastDoneTaskId;
	private RocksDatabase.Table tableTaskQueue;
	private long lastTaskId;
	private long lastDoneTaskId;
	private final byte[] lastDoneTaskIdKey = "LastDoneTaskId".getBytes(StandardCharsets.UTF_8);
	private RunTask pending;
	private AsyncSocket socket;
	// 失败/超时后的延迟重试，单flight（已在途则不重复排）。stop必须取消，否则触发时tableTaskQueue已close。
	private Future<?> retryTask;
	private static final long RETRY_DELAY_MS = 5_000;

	public RedoQueue(String name, Config config) {
		super(name, config);
	}

	@Override
	public void start() throws Exception {
		lock();
		try {
			if (db != null)
				return;
			db = new RocksDatabase(getName());
			tableLastDoneTaskId = db.getOrAddTable("FamilyLastDoneTaskId");
			tableTaskQueue = db.getOrAddTable("FamilyTaskQueue");
			try (var qit = tableTaskQueue.iterator()) {
				qit.seekToLast();
				if (qit.isValid()) {
					var last = ByteBuffer.Wrap(qit.key());
					lastTaskId = last.ReadLong();
				}
			}
			var done = tableLastDoneTaskId.get(lastDoneTaskIdKey);
			if (done != null)
				lastDoneTaskId = ByteBuffer.Wrap(done).ReadLong();
			// 排空队列重启后的水位回绕（CP1-F1）：队列排空时tableTaskQueue已无条目，
			// lastTaskId恢复为默认0，而lastDoneTaskId=N（水位表独立持久化）——水位"回绕"为
			// lastDoneTaskId>lastTaskId：泵条件lastDoneTaskId<lastTaskId永假，新增任务静默滞留；
			// 且新任务从id=1重新分配，落入水位之下的已删区间（下一次水位推进会连带误删）。
			// 钳制对齐：以水位为下界恢复lastTaskId，新增任务从N+1继续。
			lastTaskId = Math.max(lastTaskId, lastDoneTaskId);
			deleteDoneTasks(); // 清理崩溃窗口残留（水位已推进但删除未执行）
			super.start();
		} finally {
			unlock();
		}
	}

	@Override
	public void stop() throws Exception {
		lock();
		try {
			if (retryTask != null) {
				retryTask.cancel(false);
				retryTask = null;
			}
			super.stop();
			if (db != null) {
				db.close();
				db = null;
				tableLastDoneTaskId = null;
				tableTaskQueue = null;
			}
		} finally {
			unlock();
		}
	}

	public void add(int taskType, Serializable taskParam) {
		lock();
		try {
			// FND7-65：先落盘成功再推进内存lastTaskId。原先先++lastTaskId后put，put抛
			// RocksDBException时内存已前进而盘上无此任务：后续泵读lastDoneTaskId+1命中空洞
			// 永久停摆，重启后lastTaskId从DB最大键恢复，洞仍在，不可自愈。
			var newTaskId = lastTaskId + 1;
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(newTaskId);

			var task = new BQueueTask();
			task.setQueueName(getName());
			task.setPrevTaskId(newTaskId - 1);
			task.setTaskId(newTaskId);
			task.setTaskType(taskType);
			var param = ByteBuffer.Allocate(1024 + 16);
			taskParam.encode(param);
			task.setTaskParam(new Binary(param.Bytes, 0, param.WriteIndex));
			var value = ByteBuffer.Allocate(1024 + 16);
			task.encode(value);

			// 保存完整的rpc请求，重新发送的时候不用再次打包。
			tableTaskQueue.put(key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
			lastTaskId = newTaskId;
			tryStartSendNextTask(task, null);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	// FND4-43：删除水位（含）以下条目。重发只读lastDoneTaskId以上，以下条目（任务正文全量落盘）
	// 永不清理=本地RocksDB无界增长。key为8字节大端long，[key(0),key(lastDoneTaskId+1))即
	// taskId<=lastDoneTaskId的全部；与水位推进同锁同线程，重启时start()再补一次（清崩溃残留）。
	private void deleteDoneTasks() throws RocksDBException {
		var first = ByteBuffer.Allocate(8);
		first.WriteLong(0);
		var end = ByteBuffer.Allocate(8);
		end.WriteLong(lastDoneTaskId + 1);
		tableTaskQueue.deleteRange(first.Bytes, end.Bytes);
	}

	private void tryStartSendNextTask(BQueueTask add, AsyncSocket socket) throws RocksDBException {
		if (pending != null)
			return;

		if (lastDoneTaskId < lastTaskId) {
			var taskId = lastDoneTaskId + 1;
			var rpc = new RunTask();
			if (add != null && taskId == add.getTaskId())
				rpc.Argument = add; // 最近加入的就是要发送的。优化！
			else {
				// 最近加入的不是要发送的，从Db中读取。
				var key = ByteBuffer.Allocate(9);
				key.WriteLong(taskId);
				var value = tableTaskQueue.get(key.Bytes, 0, key.WriteIndex);
				if (value == null) {
					// FND7-65：水位下一跳任务在DB中不存在（历史put失败留下的空洞，或外部
					// 删数据）。洞不会自愈：重启后lastTaskId从DB最大键恢复，水位之下的洞
					// 保持，队列永久停摆且无任何日志。FATAL让运维介入（人工补洞或重置水位）。
					logger.fatal("task queue hole! queue={}, taskId={}, lastDoneTaskId={}, lastTaskId={}",
							getName(), taskId, lastDoneTaskId, lastTaskId);
					return;
				}
				rpc.Argument.decode(ByteBuffer.Wrap(value));
			}
			if (this.socket == null) {
				this.socket = socket;
				if (this.socket == null) {
					this.socket = GetSocket();
					if (this.socket == null)
						return;
				}
			}
			if (rpc.Send(this.socket, this::processRunTaskResult))
				pending = rpc;
			else {
				// Send=false（socket失效/输出背压丢包不断连）：无重连事件，复用单flight重试
				// 驱动泵；error带queue/taskId归因（任务体超outputBufferMaxSize持续失败留运维补洞）。
				logger.error("RunTask send fail, schedule retry. queue={}, taskId={}",
						getName(), rpc.Argument.getTaskId());
				scheduleRetry();
			}
		}
	}

	private long processRunTaskResult(Rpc<BQueueTask, BTaskId> rpc) throws Exception {
		lock();
		try {
			if (pending != rpc)
				return Procedure.LogicError;

			pending = null;
			if (null == tableTaskQueue)
				return Procedure.LogicError; // stop与响应回调竞态：stop持锁清理时未清pending，表已置null（同scheduleRetry内的守卫）
			if (rpc.getResultCode() == 0L || rpc.getResultCode() == Procedure.ErrorRequestId) {
				lastDoneTaskId = rpc.Result.getTaskId();
				var value = ByteBuffer.Allocate(9);
				value.WriteLong(lastDoneTaskId);
				tableLastDoneTaskId.put(lastDoneTaskIdKey, 0, lastDoneTaskIdKey.length, value.Bytes, 0, value.WriteIndex);
				deleteDoneTasks();
				tryStartSendNextTask(null, rpc.getSender());
				return 0L;
			}

			// 失败或超时：连接仍在时没有事件再驱动泵（仅add/重连会），队列会永久停摆。
			// 协议按prevTaskId幂等：超时后任务可能已被服务端应用，重发会得到ErrorRequestId并采纳服务端进度，安全。
			// NotImplement等永久配置错误下持续重试并刷warn，运维修复后队列自动继续。
			logger.warn("task fail, schedule retry. queue={}, taskId={}, resultCode={}",
					getName(), rpc.Argument.getTaskId(), rpc.getResultCode());
			scheduleRetry();
			return rpc.getResultCode();
		} finally {
			unlock();
		}
	}

	private void scheduleRetry() {
		if (retryTask != null)
			return; // 单flight：已在途的重试足够驱动泵

		retryTask = TaskSpec.ofAction(() -> {
			lock();
			try {
				retryTask = null;
				if (null == tableTaskQueue)
					return; // stop与已触发的重试竞态：stop持锁先完成（cancel拦不住已启动的任务），db已关闭
				tryStartSendNextTask(null, null);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			} finally {
				unlock();
			}
		}).scheduleNow(RETRY_DELAY_MS);
	}

	@Override
	public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);
		lock();
		try {
			tryStartSendNextTask(null, so);
		} finally {
			unlock();
		}
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, Throwable ex) throws Exception {
		super.OnSocketClose(so, ex);
		lock();
		try {
			if (socket == so)
				socket = null;
		} finally {
			unlock();
		}
	}
}
