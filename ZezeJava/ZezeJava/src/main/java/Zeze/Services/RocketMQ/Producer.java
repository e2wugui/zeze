package Zeze.Services.RocketMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
import Zeze.Transaction.Transaction;
import Zeze.Util.FuncLong;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.TaskSpec;
import Zeze.Util.TimerFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * RocketMQ 事务消息生产者：本地 Zeze 过程与半消息 COMMIT/ROLLBACK 绑定，并维护事务回查表 tSent。
 */
public class Producer extends AbstractProducer implements TransactionListener {
	private static final @NotNull Logger logger = LogManager.getLogger(Producer.class);

	// tSent过期行的保留时长（毫秒），默认7天：行须存活到 broker 事务回查窗口结束——回查对已删行
	// 答 UNKNOW（checkLocalTransaction），仍在恢复窗口内的 COMMIT 行被删即失去 COMMIT 丢失时的
	// 兜底。可经系统属性覆盖，但不得低于 TSENT_KEEP_TIME_MIN（覆盖 broker 回查总窗口的上界估计）。
	private static final String TSENT_KEEP_TIME_PROPERTY = "RocketMQ.Producer.tSentKeepTimeMillis";
	private static final long TSENT_KEEP_TIME_DEFAULT = 7L * 24 * 60 * 60 * 1000;
	// 保留时长下限（毫秒）：必须覆盖 broker 回查总窗口
	// transactionTimeOut + transactionCheckMax × transactionCheckInterval（默认参数约15分钟），
	// 取1小时（默认总窗口的约4倍）以容忍 broker 调大回查参数。
	private static final long TSENT_KEEP_TIME_MIN = 60L * 60 * 1000;
	// 每批walk的行数上限：每批独立一个事务过程删除，避免单过程长事务。
	private static final int TSENT_CLEAN_BATCH_SIZE = 1000;
	// stop 的有界排空预算：事务回查线程池在飞的 checkLocalTransaction（_tSent.selectDirty 触
	// Zeze 表）须在 stop 返回前完成——典型停机顺序 stop()→app.close()，越过即对已关表的访问。
	private static final long STOP_AWAIT_MILLIS = 10_000L;

	public final @NotNull Application zeze;
	private final @NotNull TransactionMQProducer producer;
	private final @NotNull ThreadPoolExecutor checkExecutor;
	private @Nullable TimerFuture<?> tSentCleanFuture;

	public Producer(@NotNull Application zeze, @NotNull String producerGroup, @NotNull ClientConfig clientConfig) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
		producer = new TransactionMQProducer(producerGroup);
		producer.setNamesrvAddr(clientConfig.getNamesrvAddr()); // "127.0.0.1:9876"
		producer.setTransactionListener(this);
		// 自建回查线程池保留引用：destroyTransactionEnv 只对它 shutdown() 不等待，stop 需自行有界排空。
		checkExecutor = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000),
				r -> new Thread(r, "client-transaction-msg-check-thread"));
		producer.setExecutorService(checkExecutor);
	}

	public void start() throws MQClientException {
		producer.start();
		// 每日清理tSent过期行：COMMIT路径保留的事务行若不定期删除，表会无界增长。
		if (tSentCleanFuture == null)
			tSentCleanFuture = TaskSpec.ofAction(this::cleanExpiredTSent)
					.scheduleAtPeriodNow(3, 30, 24 * 60 * 60 * 1000);
	}

	public void stop() {
		if (tSentCleanFuture != null) {
			tSentCleanFuture.cancel(false);
			tSentCleanFuture = null;
		}
		producer.shutdown();
		// destroyTransactionEnv 对注入的回查线程池只 shutdown() 不等待：在飞 checkLocalTransaction
		//（触 Zeze 表）须在 stop 返回前有界排空（典型停机顺序 stop()→app.close()，越过即对已关表的
		// 访问）。shutdown 幂等（destroyTransactionEnv 已调过），超时仅告警继续，不无限等待。
		checkExecutor.shutdown();
		try {
			if (!checkExecutor.awaitTermination(STOP_AWAIT_MILLIS, TimeUnit.MILLISECONDS))
				logger.warn("RocketMQ.Producer transaction check executor not drained in {}ms", STOP_AWAIT_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public @NotNull TransactionMQProducer getProducer() {
		return producer;
	}

	/**
	 * 发送普通消息。没有相关事务。
	 */
	public SendResult sendMessage(@NotNull Message msg) throws Exception {
		return producer.send(msg);
	}

	/**
	 * 发送消息，并且把消息跟一个事务绑定起来。仅当事务执行成功时，消息才会被发送。如果事务回滚，消息将被取消。
	 * 前置 tSent 预插行过程失败（冲突重试耗尽、库异常等）时不发送消息、不执行 procedureAction，
	 * 记 error 日志并返回 null。
	 *
	 * <p>必须在<b>环境事务之外</b>调用（调用线程不得处于运行中的 Zeze 事务内）：环境事务内调用
	 * 立即抛 {@link UnsupportedOperationException}（本方法第一行 fail-fast，先于一切副作用）。
	 * 事务联动发送（外层事务提交后再发 COMMIT）需独立设计，当前不支持。
	 */
	public @Nullable TransactionSendResult sendMessageWithTransaction(@NotNull Message msg,
																	  @NotNull FuncLong procedureAction)
			throws MQClientException {
		// 环境事务内禁发事务消息。判据与 Procedure.call() 选择嵌套路径的判据完全一致
		//（Transaction.getCurrent() 非空）：此时 executeLocalTransaction 里的本地事务走
		// savepoint 合并、不落盘，而 rocketmq-client 在其返回 COMMIT_MESSAGE 后立即向 broker
		// 发出 EndTransaction(COMMIT)——外层事务随后回滚即成"幽灵消息"（本地无变更但消息已
		// 投递），外层冲突 redo 重跑则再发一条新 UNIQ_KEY 的半消息（同一笔本地事务重复投递）。
		// 不用 isRunning() 收窄：whileCommit 回调在事务 Completed 后、线程归还前执行，
		// getCurrent() 仍非空，此时嵌套过程写进无宿主的 savepoint 同样不落盘，一样产生幽灵消息。
		if (Transaction.getCurrent() != null)
			throw new UnsupportedOperationException("sendMessageWithTransaction: 环境事务内发送事务消息会产生"
					+ "幽灵消息/重复投递（内层本地事务走嵌套 savepoint 不落盘，而 COMMIT 已先发给 broker）。"
					+ "请在 Zeze 事务外发送，或改用 sendMessage 发非事务消息。");
		var txnId = zeze.getAutoKey("RocketMQ").nextString();
		msg.setTransactionId(txnId);
		var r = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
			_tSent.insert(txnId, new BTransactionMessageResult(false, System.currentTimeMillis()));
			return 0;
		}, "RocketMQ.executeLocalTransaction")).call();
		if (r != 0) {
			logger.error("sendMessageWithTransaction: tSent pre-insert procedure fail (rc={}), message not sent."
					+ " topic={}", r, msg.getTopic());
			return null;
		}
		// txnId经arg载体传递：rocketmq-client发送半消息成功后会用UNIQ_KEY覆写msg.transactionId
		//（DefaultMQProducerImpl.sendMessageInTransaction），executeLocalTransaction无法再从msg取回txnId。
		return producer.sendMessageInTransaction(msg, new TxnAction(txnId, procedureAction));
	}

	private record TxnAction(String txnId, FuncLong action) {
	}

	@Override
	public @NotNull LocalTransactionState executeLocalTransaction(@NotNull Message msg, Object arg) {
		if (!(arg instanceof TxnAction action)) {
			logger.error("executeLocalTransaction: arg is not TxnAction: {}", arg);
			return LocalTransactionState.UNKNOW;
		}
		// UNIQ_KEY是broker回查唯一带回的事务标识（ClientRemotingProcessor把回查消息的transactionId
		// 也覆写成UNIQ_KEY），回查索引行必须以它为键，在此（半消息已发出、属性已生成）补建。
		var uniqKey = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		var r = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
			if (uniqKey != null) {
				var exist = _tSent.get(uniqKey);
				if (exist != null)
					// 同一Message对象重复发送：UNIQ_KEY复用，按首跑结果分流——
					// 无条件return 0会把首跑失败残留的result=false行也COMMIT，违背
					// "仅当事务成功才发送"。
					return exist.isResult() ? 0 : 1;
			}
			var sent = _tSent.get(action.txnId());
			if (sent == null)
				return 1;
			if (sent.isResult())
				return 0;
			var check = uniqKey == null ? null : new BTransactionMessageResult(false, System.currentTimeMillis());
			if (check != null)
				_tSent.insert(uniqKey, check);
			sent.setResult(true);
			var rc = action.action().call();
			if (rc == 0 && check != null)
				check.setResult(true);
			return rc;
		}, "RocketMQ.executeLocalTransaction")).call();

		if (r == 0)
			return LocalTransactionState.COMMIT_MESSAGE;
		if (r != 1) {
			TaskSpec.ofProcedure(zeze.newProcedure(() -> {
				_tSent.remove(action.txnId());
				if (uniqKey != null)
					_tSent.remove(uniqKey);
				return 0;
			}, "RocketMQ.executeLocalTransaction.rollback")).call();
		}
		return LocalTransactionState.ROLLBACK_MESSAGE;
	}

	/**
	 * 事务回查：行存在且 result=true 返回 COMMIT；行不存在或 result=false 返回 UNKNOW。
	 * 行不存在不回 ROLLBACK：uniqKey 行由 executeLocalTransaction 在本地过程内写入，过程提交后
	 * 才对 selectDirty 可见——本地过程耗时超过 broker 回查免疫窗口（transactionTimeOut 量级，
	 * 冲突重试/过程内长 IO 可达）时回查先到，"不存在"无法区分"过程在飞"与"从未执行/已回滚/
	 * 已被过期清理"（回查报文只有 UNIQ_KEY，无 txnId→uniqKey 链接，键设计下不可分辨），
	 * ROLLBACK 会丢弃在飞过程的半消息而本地事务随后提交成功，静默违背"仅当事务成功才发送"。
	 * UNKNOW 依赖 broker 回查策略收敛：在飞过程提交后，后续回查命中 result=true → COMMIT 救回；
	 * 行真不存在时由 broker 最大回查次数耗尽丢弃半消息，终态与 ROLLBACK 等效，仅丢弃时点
	 * 推迟（回查次数×间隔量级）。
	 */
	@Override
	public @NotNull LocalTransactionState checkLocalTransaction(@NotNull MessageExt msg) {
		var uniqKey = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		var sent = uniqKey != null ? _tSent.selectDirty(uniqKey) : null;
		if (sent == null)
			return LocalTransactionState.UNKNOW;
		return sent.isResult() ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.UNKNOW;
	}

	/**
	 * 读取 tSent 保留时长配置（系统属性 {@value TSENT_KEEP_TIME_PROPERTY}，毫秒），
	 * 低于 {@value #TSENT_KEEP_TIME_MIN} 的值钳到下限：deadline=now-keepTime 在 keepTime<=0 时
	 * 不早于 now，清理会命中所有行（含仍在回查恢复窗口内的 COMMIT 行）。钳制而非抛错：对齐
	 * PropertiesHelper 对非法配置 warn+安全回退的惯例，且本方法由周期任务调用，抛错会中断清理调度。
	 */
	static long tSentKeepTimeMillis() {
		var keepTime = PropertiesHelper.getLong(TSENT_KEEP_TIME_PROPERTY, TSENT_KEEP_TIME_DEFAULT);
		if (keepTime < TSENT_KEEP_TIME_MIN) {
			logger.warn("RocketMQ.Producer tSentKeepTimeMillis={} below floor {}, clamped (must cover broker "
					+ "transaction-check window)", keepTime, TSENT_KEEP_TIME_MIN);
			return TSENT_KEEP_TIME_MIN;
		}
		return keepTime;
	}

	/**
	 * 分批walk tSent，删除Timestamp早于保留阈值（默认now-7天）的行。
	 * walk要求事务外调用（回调逐记录加读锁），故回调只收集key、每批独立一个事务过程删除；
	 * 删除幂等（remove不存在行无效果），过程失败或异常留待下个清理周期从头重试。
	 */
	private void cleanExpiredTSent() {
		var deadline = System.currentTimeMillis() - tSentKeepTimeMillis();
		var expired = new ArrayList<String>(TSENT_CLEAN_BATCH_SIZE);
		String startKey = null;
		try {
			while (true) {
				expired.clear();
				var lastKey = _tSent.walk(startKey, TSENT_CLEAN_BATCH_SIZE, (key, value) -> {
					if (value.getTimestamp() < deadline)
						expired.add(key);
					return true; // 继续
				});
				if (!expired.isEmpty()) {
					var keys = List.copyOf(expired);
					var r = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
						for (var key : keys)
							_tSent.remove(key);
						return 0;
					}, "RocketMQ.cleanTSent")).call();
					if (r != 0)
						return; // 过程失败（冲突等），下个周期重试；已删批不回滚也无害
				}
				if (lastKey == null)
					return; // 表遍历完毕
				startKey = lastKey;
			}
		} catch (Throwable e) { // 停机竞态（表已关）等，记录后下个周期重试
			logger.error("RocketMQ.Producer.cleanTSent", e);
		}
	}
}
