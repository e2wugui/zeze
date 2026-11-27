package Zeze.Services.RocketMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Application;
import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
import Zeze.Config;
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
import org.jspecify.annotations.Nullable;

/**
 * RocketMQ 事务消息生产者：将本地 Zeze 过程与半消息 COMMIT/ROLLBACK 绑定，并维护事务回查表 tSent。
 * 契约：同一 producerGroup 全集群单活实例部署（避免回查路由到无本地 tSent 行的实例）；
 * tSent 须落在持久数据库（{@link #start()} 拒绝 Memory 库），并定期清理过期行；
 * 构造期向 Application 注册 tSent，{@link #stop()} 排空回查线程池后反注册。
 */
public class Producer extends AbstractProducer implements TransactionListener {
	private static final Logger logger = LogManager.getLogger(Producer.class);

	// 同 JVM 活实例计数：超过 1 时仅告警（兼容同进程多 app 未启动的拓扑），部署仍须保证同组单活
	private static final AtomicInteger liveInstances = new AtomicInteger();

	// tSent 保留时长配置（默认 7 天，下限 1 小时以覆盖 broker 事务回查总窗口）
	private static final String TSENT_KEEP_TIME_PROPERTY = "RocketMQ.Producer.tSentKeepTimeMillis";
	private static final long TSENT_KEEP_TIME_DEFAULT = 7L * 24 * 60 * 60 * 1000;
	private static final long TSENT_KEEP_TIME_MIN = 60L * 60 * 1000;
	// 每批清理的行数上限，每批独立事务删除以避免长事务
	private static final int TSENT_CLEAN_BATCH_SIZE = 1000;
	// 停机时等待在飞回查完成的超时，确保在关闭客户端与 Zeze 应用前完成查表和应答
	private static final long STOP_AWAIT_MILLIS = 10_000L;

	public final Application zeze;
	private final TransactionMQProducer producer;
	private final ThreadPoolExecutor checkExecutor;
	private @Nullable TimerFuture<?> tSentCleanFuture;
	// stop 幂等标志：确保 liveInstances 递减与停机动作只执行一次
	private final AtomicBoolean stopped = new AtomicBoolean();

	/**
	 * @param clientConfig 路由与身份字段会透传给内部 producer（见 {@link ClientConfigs}）
	 */
	public Producer(Application zeze, String producerGroup, ClientConfig clientConfig) {
		boolean initialized = false;
		// 仅在自身注册成功后回滚反注册，避免重复表异常时误删已有活表
		boolean registered = false;
		liveInstances.incrementAndGet();
		try {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
			registered = true;
			producer = new TransactionMQProducer(producerGroup);
			ClientConfigs.copyRoutingIdentity(clientConfig, producer);
			producer.setTransactionListener(this);
			// 保留回查线程池引用，stop 时需自行有界排空（destroyTransactionEnv 仅 shutdown 不等待）
			checkExecutor = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000),
					r -> new Thread(r, "client-transaction-msg-check-thread"));
			producer.setExecutorService(checkExecutor);
			initialized = true;
		} finally {
			if (initialized && liveInstances.get() > 1)
				logger.warn("RocketMQ.Producer: {} live instances in one process (producerGroup={}):"
						+ " concurrent live instances route broker transaction-checks across local tSent tables"
						+ " (UNKNOW -> half-message drop); ensure single live instance per producerGroup per"
						+ " process/machine (see class javadoc)", liveInstances.get(), producerGroup);
			if (!initialized) {
				liveInstances.decrementAndGet(); // 构造失败归还计数
				if (registered) {
					try {
						UnRegisterZezeTables(zeze);
					} catch (Throwable t) {
						logger.error("RocketMQ.Producer ctor rollback UnRegisterZezeTables fail."
								+ " producerGroup={}", producerGroup, t);
					}
				}
			}
		}
	}

	public void start() throws MQClientException {
		// 按配置校验 tSent 所在数据库非 Memory，避免重启丢失回查记录
		//noinspection DataFlowIssue
		var dbConf = zeze.getConfig().getDatabaseConfMap()
			.get(zeze.getConfig().getTableConf(_tSent.getName()).getDatabaseName());
		if (dbConf != null && dbConf.getDatabaseType() == Config.DbType.Memory)
			throw new IllegalStateException("RocketMQ.Producer tSent must reside in a persistent database,"
					+ " but its DatabaseConf is Memory (tSent 数据随进程重启灭失，重启前已提交本地事务的半消息"
					+ "回查恒 UNKNOW 被 broker 回查耗尽丢弃；默认DatabaseConf即Memory，漏配数据库的应用静默落入)。"
					+ "请为tSent显式配置持久库。 producerGroup=" + producer.getProducerGroup());
		producer.start();
		// 每日清理 tSent 过期行，防止回查表无界增长
		if (tSentCleanFuture == null)
			tSentCleanFuture = TaskSpec.ofAction(this::cleanExpiredTSent)
					.scheduleAtPeriodNow(3, 30, 24 * 60 * 60 * 1000);
	}

	/**
	 * 幂等停止生产者：取消清理任务、排空回查线程池、关闭客户端、反注册 tSent 并递减活实例计数。
	 * 若在同一 Application 上重建 Producer，须在 {@link Application#start()} 前构造。
	 */
	public void stop() {
		if (!stopped.compareAndSet(false, true))
			return;
		if (tSentCleanFuture != null) {
			tSentCleanFuture.cancel(false);
			tSentCleanFuture = null;
		}
		// 先排空回查线程池，确保在飞回查趁客户端通道和 tSent 仍存活时发出 COMMIT 应答
		checkExecutor.shutdown();
		try {
			if (!checkExecutor.awaitTermination(STOP_AWAIT_MILLIS, TimeUnit.MILLISECONDS))
				logger.warn("RocketMQ.Producer transaction check executor not drained in {}ms", STOP_AWAIT_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		producer.shutdown();
		// 无论反注册是否异常都继续递减活实例计数
		try {
			UnRegisterZezeTables(zeze);
		} catch (Throwable t) {
			logger.error("RocketMQ.Producer stop UnRegisterZezeTables fail. producerGroup={}", producer.getProducerGroup(), t);
		}
		liveInstances.decrementAndGet();
	}

	public TransactionMQProducer getProducer() {
		return producer;
	}

	/**
	 * 发送普通消息（无事务绑定），必须在 Zeze 环境事务外调用，否则抛出 {@link UnsupportedOperationException}。
	 * 若需事务提交后发送，请在外层事务的 whileCommit 回调中异步派发。
	 */
	public SendResult sendMessage(Message msg) throws Exception {
		// 事务内（含 whileCommit）同步投递与外层提交/回滚脱钩，会导致幽灵消息或重复投递
		if (Transaction.getCurrent() != null)
			throw new UnsupportedOperationException("sendMessage: 环境事务内发送普通消息会产生"
					+ "幽灵消息/重复投递（消息同步立即投递，与外层事务的提交/回滚脱钩）。"
					+ "请在 Zeze 事务外发送。");
		return producer.send(msg);
	}

	/**
	 * 发送事务消息并绑定本地存储过程：仅当过程成功提交时消息才提交，前置预插失败时返回 null。
	 * 必须在 Zeze 环境事务外调用，否则抛出 {@link UnsupportedOperationException}。
	 */
	public @Nullable TransactionSendResult sendMessageWithTransaction(Message msg,
																	  FuncLong procedureAction)
			throws MQClientException {
		// 事务内调用会走嵌套 savepoint 不落盘，而 COMMIT 已发往 broker，外层回滚/重试会产生幽灵或重复消息
		if (Transaction.getCurrent() != null)
			throw new UnsupportedOperationException("sendMessageWithTransaction: 环境事务内发送事务消息会产生"
					+ "幽灵消息/重复投递（内层本地事务走嵌套 savepoint 不落盘，而 COMMIT 已先发给 broker）。"
					+ "请在 Zeze 事务外发送（事务内发普通消息同样被拒，见 sendMessage）。");
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
		// 通过 arg 传递 txnId：rocketmq-client 发半消息后会用 UNIQ_KEY 覆写 msg.transactionId
		return producer.sendMessageInTransaction(msg, new TxnAction(txnId, procedureAction));
	}

	private record TxnAction(String txnId, FuncLong action) {
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		if (!(arg instanceof TxnAction(String txnId, FuncLong action1))) {
			logger.error("executeLocalTransaction: arg is not TxnAction: {}", arg);
			return LocalTransactionState.UNKNOW;
		}
		// broker 回查仅携带 UNIQ_KEY，需在此以 UNIQ_KEY 为键补建回查索引行
		var uniqKey = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		long r = TaskSpec.ofProcedure(zeze.newProcedure(() -> {
			if (uniqKey != null) {
				var exist = _tSent.get(uniqKey);
				if (exist != null)
					// 同一 Message 重复发送复用 UNIQ_KEY 时，按已有结果决定提交或回滚
					return exist.isResult() ? 0 : 1;
			}
			var sent = _tSent.get(txnId);
			if (sent == null)
				return 1;
			if (sent.isResult())
				return 0;
			var check = uniqKey == null ? null : new BTransactionMessageResult(false, System.currentTimeMillis());
			if (check != null)
				_tSent.insert(uniqKey, check);
			sent.setResult(true);
			var rc = action1.call();
			if (rc == 0 && check != null)
				check.setResult(true);
			return rc;
		}, "RocketMQ.executeLocalTransaction")).call();

		if (r == 0)
			return LocalTransactionState.COMMIT_MESSAGE;
		if (r != 1) {
			TaskSpec.ofProcedure(zeze.newProcedure(() -> {
				// 仅回收本次预插的 txnId 行，保留可能已提交的同 UNIQ_KEY 回查行
				_tSent.remove(txnId);
				return 0;
			}, "RocketMQ.executeLocalTransaction.rollback")).call();
		}
		return LocalTransactionState.ROLLBACK_MESSAGE;
	}

	/**
	 * 事务回查：tSent 命中且 result=true 返回 COMMIT；查无行或 result=false 恒返回 UNKNOW
	 * （查无行无法区分在飞事务与已回滚，故既不误 COMMIT 也不提前 ROLLBACK，交由 broker 重试收敛）。
	 */
	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		var uniqKey = msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		var sent = uniqKey != null ? _tSent.selectDirty(uniqKey) : null;
		if (sent == null)
			return LocalTransactionState.UNKNOW;
		return sent.isResult() ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.UNKNOW;
	}

	/**
	 * 读取 tSent 保留时长（系统属性 {@value TSENT_KEEP_TIME_PROPERTY}，毫秒），低于下限 {@value #TSENT_KEEP_TIME_MIN} 时告警并钳制。
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
	 * 事务外分批扫描 tSent 收集过期键，每批在独立事务中幂等删除，失败留待下周期重试。
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
