package Zeze.Services.RocketMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
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

	// tSent过期行的保留时长（毫秒），默认7天：COMMIT路径保留的行在broker事务回查窗口（秒级起步、
	// 次数有限，分钟级总窗口）过后即为死数据，7天远超任何回查窗口上界；且回查对已删行答ROLLBACK
	// （checkLocalTransaction），删除方向幂等安全。可经系统属性覆盖。
	private static final String TSENT_KEEP_TIME_PROPERTY = "RocketMQ.Producer.tSentKeepTimeMillis";
	private static final long TSENT_KEEP_TIME_DEFAULT = 7L * 24 * 60 * 60 * 1000;
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
	 */
	public @Nullable TransactionSendResult sendMessageWithTransaction(@NotNull Message msg,
																	  @NotNull FuncLong procedureAction)
			throws MQClientException {
		var txnId = zeze.getAutoKey("RocketMQ").nextString();
		msg.setTransactionId(txnId);
		var r = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
			_tSent.insert(txnId, new BTransactionMessageResult(false, System.currentTimeMillis()));
			return 0;
		}, "RocketMQ.executeLocalTransaction")).call();
		// txnId经arg载体传递：rocketmq-client发送半消息成功后会用UNIQ_KEY覆写msg.transactionId
		//（DefaultMQProducerImpl.sendMessageInTransaction），executeLocalTransaction无法再从msg取回txnId。
		return r == 0 ? producer.sendMessageInTransaction(msg, new TxnAction(txnId, procedureAction)) : null;
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

	@Override
	public @NotNull LocalTransactionState checkLocalTransaction(@NotNull MessageExt msg) {
		var uniqKey = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		var sent = uniqKey != null ? _tSent.selectDirty(uniqKey) : null;
		if (sent == null)
			return LocalTransactionState.ROLLBACK_MESSAGE;
		return sent.isResult() ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.UNKNOW;
	}

	/**
	 * 分批walk tSent，删除Timestamp早于保留阈值（默认now-7天）的行。
	 * walk要求事务外调用（回调逐记录加读锁），故回调只收集key、每批独立一个事务过程删除；
	 * 删除幂等（remove不存在行无效果），过程失败或异常留待下个清理周期从头重试。
	 */
	private void cleanExpiredTSent() {
		var deadline = System.currentTimeMillis()
				- PropertiesHelper.getLong(TSENT_KEEP_TIME_PROPERTY, TSENT_KEEP_TIME_DEFAULT);
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
