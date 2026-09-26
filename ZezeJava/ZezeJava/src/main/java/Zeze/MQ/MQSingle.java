package Zeze.MQ;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.PushMessage;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Action0;
import Zeze.Util.OutLong;
import Zeze.Util.RocksDatabase;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static Zeze.MQ.Master.AbstractMaster.eConsumerNotFound;

public class MQSingle extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger();

	// null-manager 测试形态的配置默认值（只读使用，不改）。
	private static final MQConfig DEFAULT_CONFIG = new MQConfig();

	private final String topic;
	private final int partitionIndex;
	private long bindSessionId;
	private @Nullable AsyncSocket bindSocket;
	private @Nullable PushMessage pendingPushMessage;
	private final MQPartition mqPartition;
	private final MQFileWithIndex fileWithIndex;
	private long highLoad;
	private final AtomicLong loadCounter = new AtomicLong();
	private long lastLoadCounter;
	private long lastReportTime = System.currentTimeMillis();

	// 【GB-D06】队头消息连续投递失败次数（首推为 0，失败一投 +1；成功/转死信出队即清零）。
	// 单飞+队头peek保证失败必是队头同一条；仅锁内读写。
	private int headRetryCount;
	// 【GB-D06】退避窗口标志（true=退避中，tryPushMessage 整体暂停）与排期句柄（close 取消用）。
	// 分开表达：窗口以标志为准，句柄只管取消（同步执行的测试调度器下，动作先于句柄赋值跑完）。
	private boolean retryPending;
	private @Nullable Future<?> retryFuture;
	// 【GB-D06】包内可见：退避重推调度器（默认 TaskSpec 延迟调度，DaemonTimer 同形态）；
	// 测试注入捕获延迟序列/同步执行以测退避形态。
	@FunctionalInterface
	interface RetryScheduler {
		Future<?> schedule(long delayMs, Action0 action) throws Exception;
	}
	@NotNull RetryScheduler retryScheduler =
			(delayMs, action) -> TaskSpec.ofAction(action).name("MQSingle.retryPush").scheduleNow(delayMs);

	// 【FND20 GB-D03】fill 失败自排期（拍板 A：失败点排期，替代纯事件驱动）：回收竞态/瞬时 IO 错
	// 制造"队列空+无 ack 在途+无新消息"的无事件源窗口，FND19 修复刻意依赖的 sendMessage/ack
	// 事件重试前提失效，分区在消费者健康在线时无限期停摆。失败自身成为下一个事件——恢复链
	// 闭合在故障点。退避复用 retryBackoffMs 公式（连续失败指数增长、封顶，确定性损坏下重试
	// 频率有界=退避封顶，error 每周期一条即案卷要求的周期性提醒）；迟到排期幂等
	// （tryStartBackgroundFill 的 highLoad>0/stopped 检查自然短路），单槽句柄 pending 数自限。
	// 无窗口标志（区别于 retryPending）：tryStartBackgroundFill 自身幂等，只需句柄管理。
	private int fillFailCount;
	private @Nullable Future<?> fillRetryFuture;
	// 包内可见：测试注入捕获延迟序列/手动驱动（同 retryScheduler 缝；默认形态亦同）。
	@NotNull RetryScheduler fillRetryScheduler =
			(delayMs, action) -> TaskSpec.ofAction(action).name("MQSingle.fillRetry").scheduleNow(delayMs);
	// 分区关闭标志（lock内写）：close 后拒绝新的后台回填提交。排空超预算逃逸的晚到 fill 在
	// catch 里还会自排期重试，无此标志则分区删除/停机后僵尸重试循环（60s周期）触碰已关闭的
	// 文件流与 rocksdb——managerStopped 不覆盖"分区删除而 Manager 存活"路径（removePartition
	// →close 不置 manager.stopped）。
	private boolean closed;

	public static final int maxFillMessageCount = 4 * 1024;

	private final Queue<BMessage.Data> messageQueue = new ConcurrentLinkedQueue<>();
	private volatile Future<?> messageFillFuture;
	//private final Future<?> fillGuardTimer;

	public MQPartition getMQPartition() {
		return mqPartition;
	}

	public MQSingle(MQPartition partition, String topic, int partitionId) {
		this(partition, topic, partitionId, createFileWithIndex(partition, topic, partitionId));
	}

	private static MQFileWithIndex createFileWithIndex(MQPartition partition, String topic, int partitionId) {
		try {
			return new MQFileWithIndex(
					partition.getManager().getHome(),
					partition.getManager().getRocksDatabase(),
					topic, partitionId);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// 包内可见：测试注入MQFileWithIndex（见ZezeJavaTest的TestMQSingleDirectEnqueue）；行为与公有构造一致。
	MQSingle(MQPartition partition, String topic, int partitionId, MQFileWithIndex fileWithIndex) {
		this.mqPartition = partition;
		this.topic = topic;
		this.partitionIndex = partitionId;
		try {
			this.fileWithIndex = fileWithIndex;
			this.highLoad = fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId();
			pullMessage(true); // 构造的时候还没有绑定网络，所以只装载进来，不需要tryPushMessage.
			//fillGuardTimer = Task.scheduleNow(5_000, 5_000, this::tryStartBackgroundFill);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public double load() {
		var now = System.currentTimeMillis();
		var elapse = (now - lastReportTime) / 1000.0f;
		lastReportTime = now;
		var load = loadCounter.get();
		var diffLoad = load - lastLoadCounter;
		if (diffLoad > 0) {
			lastLoadCounter = load;
			return diffLoad / elapse;
		}
		return 0.0;
	}

	public void sendMessage(BSendMessage.Data message) {
		lock();
		try {
			// 停机窗口的竞态收口：handler 入口的 stopped 检查通过后 stop() 仍可能先完成（worker
			// 池任务滞后执行），这里在锁内复查——close 持同锁关文件流后，晚到任务必经此处拒绝。
			if (managerStopped())
				throw new IllegalStateException("mq manager stopped, reject sendMessage. topic=" + topic);
			// 【不变量】内存队列必须恰好是盘上积压[firstMessageId,nextMessageId)的连续前缀
			// （队头id==firstMessageId）。仅当队列已装载全部积压时才允许直入：此时盘上没有
			// 待回填消息，也不存在还会向队尾追加的后台回填（回填一旦还有消息未装载完，
			// 队列大小必小于积压数，条件不成立），直入不会破坏顺序。判断必须在appendMessage
			// 之前做（append会推进nextMessageId）。
			// 旧条件highLoad==0在"pullMessage已用calculateFill把highLoad减到0、但锁外
			// fillMessage还没装载完盘上积压"的窗口内也成立，此时直入会插到积压之前：
			// 分区内乱序，且ack后increaseFirstMessageId盲目+1越过未投递消息，重启后消息永久丢失。
			// nextMessageId/firstMessageId的所有写点（appendMessage/increaseFirstMessageId）
			// 都在本锁内执行，这里锁内读取是精确的。
			var directEnqueue = messageQueue.size() < maxFillMessageCount
					&& messageQueue.size() == fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId();
			fileWithIndex.appendMessage(message.getMessage());
			if (directEnqueue) {
				// 低负载，缓冲足够大，直接进入缓冲。
				messageQueue.offer(message.getMessage());
			} else {
				highLoad++;
				// 盘上出现未装载积压：尝试启动回填。fill 失败复位后的重试事件源除了 ack 回调，
				// 还需要这里——队列耗尽后 ack 链不再产生事件，只有新消息能重新驱动回填。
				tryStartBackgroundFill();
			}
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	private void tryStartBackgroundFill() {
		lock();
		try {
			// stopped 后不得再提交新 fill：close 的有界排空只等待已存在的 future，之后新提交的
			// 任务会与 rocksDatabase.close 并发（native use-after-free）。
			// closed（分区级，【FND20 GB-D03】）：分区删除（removePartition→close）而 Manager 存活
			// 时 stopped 恒 false，无此检查则晚到的 fill 重排期会在已关闭的文件流/rocksdb 上重试。
			if (!closed && !managerStopped() && highLoad > 0 && messageFillFuture == null && messageQueue.size() < maxFillMessageCount / 2)
				messageFillFuture = TaskSpec.ofAction(this::pullMessage).name("pullMessage").submitNow();
		} finally {
			unlock();
		}
	}

	// 包内可见：测试在确定位置同步驱动一次后台回填（见ZezeJavaTest的TestMQSingleDirectEnqueue）。
	void pullMessage() {
		pullMessage(false);
	}

	// fromConstructor=true：构造直调装载。失败保持"创建失败"的响亮语义上抛，不为僵尸分区排期
	// 自重试（【FND20 GB-D03】构造路径豁免——构造抛出后实例不发布，自排期只会重试一个
	// 无人引用的半成品分区）。
	private void pullMessage(boolean fromConstructor) {
		// 在另一个线程中调用，但只有一个线程任务。
		var first = new OutLong();
		var last = new OutLong();
		try {
			lock();
			try {
				if (highLoad > 0) {
					// calculateFill 里面还会加fileWithIndex的锁. 两把锁得到一个快照。
					highLoad -= fileWithIndex.calculateFill(messageQueue, first, last, maxFillMessageCount);
				}
			} finally {
				unlock();
			}
			fileWithIndex.fillMessage(messageQueue, first.value, last.value);
			// 这里有一个时间窗口：刚刚fill的消息全部都消费完毕，下面才置空，导致fill停止。
			messageFillFuture = null; // 这个清除没加锁
			fillFailCount = 0; // 【FND20 GB-D03】装载成功即清失败计数（瞬时失败自愈的基线复位；同上行不加锁，读侧容忍陈旧值）
			tryStartBackgroundFill(); // 这个调用是为了解决上面的时间窗口的。
		} catch (RuntimeException e) {
			// fill 失败必须复位 messageFillFuture 并重算 highLoad，否则 tryStartBackgroundFill 永远
			// 看到非null而跳过，该分区回填永久停摆。calculateFill 已按快照扣减 highLoad 但装载
			// 未完成，按盘上真实积压重算；next/first 的所有写点（appendMessage/increaseFirstMessageId）
			// 都在本锁内执行，锁内读取是精确的。不立即重启 fill（确定性数据损坏时避免紧密
			// 循环），由后续 sendMessage/ack 事件驱动重试。
			lock();
			try {
				highLoad = fileWithIndex.getNextMessageId() - fileWithIndex.getFirstMessageId() - messageQueue.size();
				messageFillFuture = null;
				// 【FND20 GB-D03】失败点自排期：事件驱动的前提在"队列空+无 ack 在途+无新消息"
				// 窗口失效（回收竞态/瞬时 IO 错的典型形态），失败自身成为下一个事件。锁内排期：
				// 与 close 的句柄取消串行化（排空超预算逃逸的晚到 fill 在 close 后排期，会被
				// closed 检查在触发时短路）。
				if (!fromConstructor)
					scheduleFillRetry();
			} finally {
				unlock();
			}
			logger.error("pullMessage fill failed. topic={} partition={}", topic, partitionIndex, e);
			throw e; // 后台任务路径异常由 TaskBody 记日志后吞掉；构造路径保持创建失败的响亮语义。
		}
		// fill 装载完成后，消息队列从空变为非空时（例如ack回调触发fill时队列已空），无人驱动推送，
		// 这里主动尝试推送；构造函数路径 bindSocket==null 时自然短路。
		lock();
		try {
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	// 【FND20 GB-D03】失败点自排期（pullMessage 的 catch 锁内调用）：指数退避后重排一次
	// tryStartBackgroundFill。不立即重排（无退避）正是 FND19 注释明示要避免的确定性损坏紧密
	// 循环；退避封顶使确定性损坏下重试周期有界（默认60s）。单槽句柄：新失败 cancel+replace
	// 旧排期，pending 数自限。
	private void scheduleFillRetry() {
		var delayMs = retryBackoffMs(++fillFailCount, config());
		if (null != fillRetryFuture)
			fillRetryFuture.cancel(false);
		try {
			fillRetryFuture = fillRetryScheduler.schedule(delayMs, this::runFillRetry);
		} catch (Exception e) {
			fillRetryFuture = null;
			// 调度失败（调度池关闭等停机窗口）：自排期缺失，退回既有的事件驱动重试兜底
			// （后续 sendMessage/ack 照常触发 tryStartBackgroundFill，进程停机时自然静默）。
			logger.error("mq fill retry schedule failed, backoff skipped. topic={} partition={}",
					topic, partitionIndex, e);
		}
	}

	// 退避到期：重排一次回填（幂等短路见 tryStartBackgroundFill）。不在此清计数——只有装载
	// 成功才复位（连续失败持续指数退避）；close 取消后迟到触发被 closed/managerStopped 检查短路。
	private void runFillRetry() {
		tryStartBackgroundFill();
	}

	private void tryPushMessage() {
		// 【GB-D06】退避窗口：分区推送整体暂停（含 sendMessage/ack 等事件触发的重推）——
		// 保序的代价（队头毒消息挡住后继，见 onPushFailure），窗口由 retryPending 表达。
		if (retryPending)
			return;
		if (null == pendingPushMessage && !messageQueue.isEmpty() && bindSocket != null) {
			pendingPushMessage = new PushMessage();
			pendingPushMessage.Argument.setTopic(topic);
			pendingPushMessage.Argument.setPartitionIndex(partitionIndex);
			pendingPushMessage.Argument.setSessionId(bindSessionId);
			// 【GB-D06】重投计数随推送下发（首推=0，每失败一投+1；消费者忽略本字段）。
			pendingPushMessage.Argument.setRetryCount(headRetryCount);
			var message = messageQueue.peek();
			pendingPushMessage.Argument.setMessage(message);
			// 推送超时必须走 MQConfig.RpcTimeout（默认 20s）：此前不传超时固定按 Rpc 字段默认
			// 5000ms，慢消费者场景每 5 秒被重推一次，配置的 RpcTimeout 只流入 Raft ProxyServer
			// 的代理路径、从不作用于 MQ 数据面。
			if (!pendingPushMessage.Send(bindSocket, (p) -> {
				handlePushResult();
				return 0;
			}, mqPartition.getManager().getMqConfig().getRpcTimeout())) {
				// Send失败（连接失效）时回调不会被调用，必须在这里清理，
				// 否则pendingPushMessage永久悬挂，该分区消息投递永久停止，bind()也无法恢复。
				pendingPushMessage = null;
			}
		}
	}

	// 包内可见：推送ack回调体（测试同步模拟回调到达，见ZezeJavaTest的TestMQSingleAckCallbackStall）。
	// 持本锁执行，锁内再进fileWithIndex锁，与calculateFill的锁序一致。
	void handlePushResult() {
		lock();
		try {
			// 停机窗口：socket 关闭会使在飞 rpc 的超时回调照常触发（Service.stop 不清 _RpcContexts），
			// 此处不触碰 rocksdb 位点（close 已持锁排空在飞写，pending 随分区关闭一并丢弃）。
			// 分区删除窗口（【FND21 GB-C01】，Manager 存活——GB-D01 对账链的常态产物，stopped 恒
			// false）：closed 只在 close 锁内置位，本回调持同锁读即精确。removePartition→close 并不
			// 取消在飞 pendingPushMessage（rpc 上下文仍在 proxyServer），其后的 deletePartitionStorage
			// 将 dropTable meta/索引列族并按前缀清理 dlq——晚到的 ack 再触 increaseFirstMessageId 的
			// meta.put 是对已毁句柄的 native 写（RocksDatabase.dropTable 销毁句柄的契约），失败分支
			// tryDeadLetter 的 dlq.put 落在 deleteRange 之后则复活"分区已删却永无人认领"的孤儿死信键
			//（FND20 GB-D02 要消灭的跨代际残留形态）。两闸同点收口；at-least-once 无损：分区正在
			// 删除，位点丢失是删除的既定语义；pending 复位与续推由 finally/bindSocket=null 自然兜底
			//（tryPushMessage 对已关分区恒短路）。
			if (managerStopped() || closed)
				return;
			loadCounter.incrementAndGet(); // 处理失败也进行计数。

			if (pendingPushMessage.getResultCode() == 0) {
				// 先持久化推进位点再出队：increaseFirstMessageId 抛异常（如rocksdb写失败）时消息
				// 留在队首、位点未推进（内存位点在持久化成功后才前移），下面finally重推的就是
				// 同一条（at-least-once），位点不会跳过它。
				fileWithIndex.increaseFirstMessageId();
				messageQueue.poll();
				headRetryCount = 0; // 【GB-D06】队头换消息，重投计数清零
				tryStartBackgroundFill();
			} else if (pendingPushMessage.getResultCode() == eConsumerNotFound) {
				// 幽灵订阅自愈：消费端条目已删（退订 best-effort 失败/超时遗留），继续重推只会
				// 永远收到 eConsumerNotFound 空转，该分区位点永不推进——清掉 Manager 侧订阅并重排。
				// 必须异步执行：arrangeConsumer 持 MQPartition 锁后经 partition.bind() 进各 MQSingle
				// 锁，这里正持本 MQSingle 锁，同步反向取 MQPartition 锁构成 AB-BA 死锁。
				// 会话标识取 pending 里发送时记录的值而非当前 bind 字段：推送在途期间分区可能已
				// 重绑到新会话，按当前字段清理会误杀新订阅。
				var ghostSessionId = pendingPushMessage.Argument.getSessionId();
				var ghostSocket = pendingPushMessage.getSender();
				TaskSpec.ofAction(() -> mqPartition.unsubscribe(ghostSocket, ghostSessionId))
						.name("MQSingle.unsubscribeGhost")
						.submitNow();
			} else {
				// 【GB-D06】其余非0结果=投递失败（消费端异常/超时）：计数、退避/死信处置。
				onPushFailure();
			}
		} finally {
			// 不管推送成功失败，都复位pending并尝试重新pushMessage（【GB-D06】退避窗口内
			// tryPushMessage 自行短路，等价"延迟重试"）。
			// 必须必达：本次rpc上下文已消费、不会再有第二次回调，上面任何异常跳过这里都会使
			// pending永久悬挂，该分区推送永久停止。
			try {
				pendingPushMessage = null;
				tryPushMessage();
			} finally {
				unlock();
			}
		}
	}

	/** 【GB-D06】投递失败处置（handlePushResult 锁内调用，队头即失败消息：单飞+队头peek）。 */
	private void onPushFailure() {
		var config = config();
		++headRetryCount;
		if (headRetryCount >= config.getPushRetryMax()) {
			// 达上限：位点照常推进 + 消息转终态（死信/按配置丢弃），不再阻塞队头；
			// 死信写失败时不推进（消息留在队首按退避重推，at-least-once 不破）。
			var messageId = fileWithIndex.getFirstMessageId();
			var message = messageQueue.peek();
			if (tryDeadLetter(messageId, message)) {
				fileWithIndex.increaseFirstMessageId();
				messageQueue.poll();
				headRetryCount = 0;
				tryStartBackgroundFill(); // 队头出队腾出空间，续装载（成功路径同款）
				return;
			}
		}
		// 未达上限或死信写失败：按次数指数退避延迟重推（原 20s 周期无上限永久重推的毒消息环取消）。
		scheduleRetryPush(retryBackoffMs(headRetryCount, config));
	}

	// 指数退避：min(Cap, Base << retryCount)，移位钳制 21 位防溢出（Cap 先钳住，实际到不了）。
	// 退避封顶的理由：退避期间该分区不推任何消息（保序代价），一条毒消息阻塞分区的时长
	// = Σ退避 + ΣRpcTimeout，退避不封顶则上限后才能转死信的等待无界。
	static long retryBackoffMs(int retryCount, MQConfig config) {
		var shift = Math.min(retryCount, 21);
		var delay = config.getPushRetryBackoffBaseMs() << shift;
		return Math.min(delay, config.getPushRetryBackoffCapMs());
	}

	private void scheduleRetryPush(long delayMs) {
		if (retryPending)
			return; // 已在退避窗口（本应不可达：窗口内无推送在飞），保持窗口不重排
		retryPending = true;
		try {
			retryFuture = retryScheduler.schedule(delayMs, this::runRetryPush);
		} catch (Exception e) {
			// 调度失败（调度池关闭等停机窗口）：撤销窗口位，退避缺失但链路存活——
			// 后续 sendMessage/ack 事件照常触发 tryPushMessage（进程停机时自然静默）。
			retryPending = false;
			logger.error("mq retry push schedule failed, backoff skipped. topic={} partition={}", topic, partitionIndex, e);
		}
	}

	// 退避到期：清窗口位并重推（可能与 close 取消竞争——取消后本动作迟到到达时分区已关闭，
	// tryPushMessage 的 bindSocket/pending 检查自然短路；删除路径已先 bind(0,null)）。
	private void runRetryPush() {
		lock();
		try {
			retryPending = false;
			retryFuture = null;
			tryPushMessage();
		} finally {
			unlock();
		}
	}

	/**
	 * 【GB-D06】转死信（或按配置丢弃）。死信键值见 {@link MQManager#DlqTableName} 侧注释：
	 * key=binary(topic,partitionIndex,messageId)，value=BMessage 编码+8字节BE时间戳。
	 *
	 * @return true=终态落定（死信已写/策略为丢弃），可推进位点；false=死信写失败不得推进。
	 */
	private boolean tryDeadLetter(long messageId, @Nullable BMessage.Data message) {
		var config = config();
		// 元数据摘要（不落消息体：协议上限 100MB 级，整包进日志有运维风险）
		var meta = "topic=" + topic + " partition=" + partitionIndex + " messageId=" + messageId
                + " retryCount=" + headRetryCount + " timestamp=" + (null != message ? message.getTimestamp() : -1)
                + " bodySize=" + (null != message ? message.getBody().size() : 0);
		if (config.isPushDiscardPolicy()) {
			logger.warn("mq poison message dropped (policy=discard, at-least-once 放弃投递的显式决策). {}", meta);
			return true;
		}
		if (null == message)
			return false; // 队列与位点竞态下的防御（正常不可达）：不推进，留下轮重推
		var manager = mqPartition.getManager();
		if (null == manager)
			return false; // null-manager 测试形态：无死信存储，不推进（留下轮重推，不进终态）
		try {
			var key = MQManager.dlqKey(topic, partitionIndex, messageId); // 编码单点（重放/清理联动共用）
			var value = ByteBuffer.Allocate();
			message.encode(value);
			// 尾缀 8 字节 BE 死信时间戳（longBeHandler，与 MQFileWithIndex 的位点编码同款）。
			var stamped = java.util.Arrays.copyOfRange(value.Bytes, value.ReadIndex, value.ReadIndex + value.size() + 8);
			ByteBuffer.longBeHandler.set(stamped, stamped.length - 8, System.currentTimeMillis());
			var dlq = manager.getDlqTable();
			dlq.put(key, 0, key.length, stamped, 0, stamped.length);
			logger.warn("mq poison message dead-lettered (at-least-once 放弃投递的显式决策，可审计可重放). {}", meta);
			enforceDlqCap(dlq, config); // 【FND20 GB-D02】保留上界：写入点就近检查（全本地，不挂上报链）
			return true;
		} catch (Exception e) {
			logger.error("mq dead letter write failed, keep message at head and retry. {}", meta, e);
			return false;
		}
	}

	// 【FND20 GB-D02】dlq 保留上界（DlqMaxEntries，estimate 口径近似）：写入后以 estimate-num-keys
	//（O(1)）检查，超限按键序迭代淘汰至目标线并 warn（淘汰动作=可审计的告警面）。全本地且锁内
	// 低频可付（迭代≤上限条数、毫秒级，只在溢出时发生——毒消息化本身是分钟级低频事件）；不挂
	// loadMonitorTimer——本地动作挂远端链是 GB-D01(FND20) 同型教训。键序≠时间序，淘汰跨 topic
	// 任举但确定（如实声明，不做时间序）。
	private void enforceDlqCap(RocksDatabase.Table dlq, MQConfig config) {
		try {
			var count = dlq.getKeyNumbers();
			var max = config.getDlqMaxEntries();
			if (count <= max)
				return;
			var evicted = 0;
			try (var it = dlq.iterator()) {
				it.seekToFirst();
				while (it.isValid() && count - evicted > max) {
					dlq.delete(it.key());
					++evicted;
					it.next();
				}
			}
			if (evicted > 0)
				logger.warn("mq dlq over cap, evicted {} oldest-by-key entries (estimate {} > DlqMaxEntries {})"
								+ ". topic={} partition={}", evicted, count, max, topic, partitionIndex);
		} catch (Exception e) {
			// 上界执法失败不回滚写入也不重抛（消息终态已落定，at-least-once 语义不变）：
			// 下一次写入再检查，dlq 有界性由后续淘汰收敛。
			logger.error("mq dlq cap enforcement failed. topic={} partition={}", topic, partitionIndex, e);
		}
	}

	private MQConfig config() {
		var manager = mqPartition.getManager();
		return null != manager ? manager.getMqConfig() : DEFAULT_CONFIG;
	}

	public void bind(long sessionId, AsyncSocket socket) {
		lock();
		try {
			this.bindSessionId = sessionId;
			this.bindSocket = socket;
			if (null != bindSocket)
				tryPushMessage();
		} finally {
			unlock();
		}
	}

	public String getTopic() {
		return topic;
	}

	// 【GB-D01/GB-D06】包内可见：测试读位点/驱动删除与毒消息路径断言。
	MQFileWithIndex getFileForTest() {
		return fileWithIndex;
	}

	// 【GB-D02】包内可见：loadMonitorTimer 周期驱动的段回收透传（fileWithIndex 私有；
	// 回收自判条件与锁序见 MQFileWithIndex.tryRecycle，无需本类锁——其全部写点在fileWithIndex锁内）。
	void tryRecycleSegments(long delayMs) {
		fileWithIndex.tryRecycle(delayMs);
	}

	public int getPartitionIndex() {
		return partitionIndex;
	}

	// 测试可用 null manager 构造，这里 null 容忍。
	private boolean managerStopped() {
		var manager = mqPartition.getManager();
		return null != manager && manager.isStopped();
	}

	public void close() throws IOException {
		//fillGuardTimer.cancel(true);
		// 停机排空在飞回填（预算式，对齐 loadMonitorTimer 的 RpcTimeout 量级+余量）：fill 任务在
		// 锁外持索引迭代器与文件读，与 rocksDatabase.close 并发属 native use-after-free
		// （RocksDatabase.close 契约）。超预算仅告警继续（与 Application 停机的有界等待口径一致，
		// 不引入无限等待）。
		// 读取必须在分区锁内（增量审R1-01）：tryStartBackgroundFill 的 stopped检查+future赋值
		// 持同锁原子——锁内读要么看到已提交的future（排空它），要么读到null且此后新提交必被
		// stopped拒绝（stop先于queue.close置位），消除锁外读漏掉临界提交的窗口。
		Future<?> fill;
		lock();
		try {
			fill = messageFillFuture;
		} finally {
			unlock();
		}
		if (null != fill) {
			var manager = mqPartition.getManager();
			var budgetMs = (null != manager ? manager.getMqConfig().getRpcTimeout() : 20_000) + 5_000L;
			try {
				fill.get(budgetMs, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				logger.warn("mq fill task not drained in {}ms, continue close. topic={} partition={}",
						budgetMs, topic, partitionIndex);
			} catch (ExecutionException e) {
				// fill 自身失败已在 pullMessage 的 catch 记录日志。
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("mq fill drain interrupted, continue close. topic={} partition={}", topic, partitionIndex);
			}
		}
		// 持锁关文件流：与在飞 sendMessage（appendMessage 同锁）串行；close 过后晚到的任务在
		// 锁内复查 stopped 拒绝，不再触碰文件与 rocksdb。【GB-D06】一并取消退避重推排期并静默
		// 绑定（迟到触发的 runRetryPush 因 bindSocket=null 自然短路）。【FND20 GB-D03】一并取消
		// fill 自排期句柄并置 closed（迟到的 runFillRetry 在 tryStartBackgroundFill 被 closed
		// 检查短路，不再提交新 fill）。
		lock();
		try {
			closed = true;
			if (null != retryFuture) {
				retryFuture.cancel(false);
				retryFuture = null;
			}
			if (null != fillRetryFuture) {
				fillRetryFuture.cancel(false);
				fillRetryFuture = null;
			}
			bindSocket = null;
			fileWithIndex.close();
		} finally {
			unlock();
		}
		// 【FND21 GB-C02】世代排空：上面的排空只等待锁内读到的一代 future——等待期间 fill 完成路径
		// 会自提交下一代（pullMessage 尾部 messageFillFuture=null 后紧跟 tryStartBackgroundFill，此刻
		// closed 尚未置位；分区删除路径 Manager 存活、无 stopped 兜底，handlePushResult 成功路径与
		// sendMessage 同样在锁内提交），置闸段不重读不取消 messageFillFuture——逃逸代立即开跑的
		// fillMessage（索引迭代器+段文件读）与其后 deletePartitionStorage 的 dropTable 并发是
		// native use-after-free。置 closed 后循环重读+锁外有界等待：closed 先行使 tryStartBackgroundFill
		// 从此恒拒绝（检查与赋值同锁），每代 future 在成功/失败路径均自清 messageFillFuture，
		// 循环必收敛（读到 null 即终态，不再有新提交）。等待必须锁外：fill 任务体收尾的
		// tryStartBackgroundFill/tryPushMessage 需要本锁，持锁等待是必然超时的自阻。
		// 超预算仅告警放弃（与上段同口径，不引入无限等待）。
		while (true) {
			Future<?> escapee;
			lock();
			try {
				escapee = messageFillFuture;
			} finally {
				unlock();
			}
			if (null == escapee)
				break;
			var manager = mqPartition.getManager();
			var budgetMs = (null != manager ? manager.getMqConfig().getRpcTimeout() : 20_000) + 5_000L;
			try {
				escapee.get(budgetMs, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				logger.warn("mq fill escapee not drained in {}ms, continue close. topic={} partition={}",
						budgetMs, topic, partitionIndex);
				break;
			} catch (ExecutionException e) {
				// fill 自身失败已在 pullMessage 的 catch 记录日志。
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("mq fill escapee drain interrupted, continue close. topic={} partition={}",
						topic, partitionIndex);
				break;
			}
		}
	}
}
