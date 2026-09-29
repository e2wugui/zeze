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
 *
 * <p>部署契约（本桥事务回查设计的硬约束，违反即静默丢消息）：
 * <ul>
 * <li>承载事务消息的 producerGroup 必须<b>全集群单实例</b>（每 JVM 至多一个 Producer，跨机每个
 * producerGroup 只部署一个进程实例）：broker 的事务回查按 producerGroup 从组内<b>任一</b>存活
 * producer 通道中选一个发送 CHECK_TRANSACTION_STATE，而回查依据的 tSent 是随<b>本进程</b>
 * Application 注册的本地表——组内出现第二个实例时，回查可能路由到没有该行的实例恒答 UNKNOW，
 * 半消息最终被回查次数耗尽丢弃（本地事务已成功提交而消息灭失，且发送方对 endTransaction
 * 丢失本就无感）。进程内多活实例由构造时计数告警（多 app 同进程的惰性拓扑不阻断，见 liveInstances）；
 * 跨机形态无法在进程内防御，必须由部署保证。</li>
 * <li>tSent 生命周期：executeLocalTransaction 提交时随业务事务落盘（result=true 的行即
 * "本地事务已成功"的证据），由周期清理按 {@link #tSentKeepTimeMillis()}（默认7天，下限1小时）
 * 删除过期行——保留时长必须覆盖 broker 回查总窗口，否则 COMMIT 行被提前清理后，endTransaction
 * 丢失的半消息失去回查兜底。</li>
 * <li>回查查无行恒答 UNKNOW 的语义（见 {@link #checkLocalTransaction}）：既不答 COMMIT 也不答
 * ROLLBACK，收敛依赖 broker 回查策略 + tSent 保留时长下界。</li>
 * <li>生命周期配对：构造器把 tSent 注册进 Application，{@link #stop()} 反注册并关闭它——
 * 同一 Application 上 stop 后<b>直接</b>重建 Producer 即可（构造器重新注册）。重建实例须
 * 在 app start 前构造（start 只打开当时已注册的表）；app 存续期间"只停不重建"的形态由
 * stop 自行关表，app 收尾无泄漏。</li>
 * </ul>
 */
public class Producer extends AbstractProducer implements TransactionListener {
	private static final @NotNull Logger logger = LogManager.getLogger(Producer.class);

	// 同 JVM 多实例计数（FND29 rocketmq-02 告警面）：多 app 同进程（如 Infinite.Simulate 五 app
	// 拓扑）仅构造未 start 的实例是惰性的，硬拒绝会把合法拓扑一刀切死（终验 Simulate 级联
	// 139 败实锤）——降级为 warn 保留可观测性；真正危险的是多实例同时在线（同组回查路由
	// 串本地 tSent 台），部署契约见类 javadoc，跨机形态本进程无法防御。
	private static final @NotNull AtomicInteger liveInstances = new AtomicInteger();

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
	// stop 幂等标志：stop 里 shutdown 族调用天然幂等，唯 liveInstances 递减不是——
	// 重复 stop 必须由本标志 CAS 抢占整体只执行一次，否则计数下漂瓦解多实例告警判据。
	private final @NotNull AtomicBoolean stopped = new AtomicBoolean();

	/**
	 * @param clientConfig 传入即生效：namesrvAddr/namespace/instanceName 等路由/身份字段透传给
	 *                     内部 producer（见 {@link ClientConfigs}），未列字段可经 {@link #getProducer()} 设置。
	 */
	public Producer(@NotNull Application zeze, @NotNull String producerGroup, @NotNull ClientConfig clientConfig) {
		boolean initialized = false;
		// 注册成功标记：失败回滚只反注册自己成功注册过的表——RegisterZezeTables自身抛出
		//（duplicate table，注册未发生）时反注册会摘掉注册者的活表（removeTable按id/name删）。
		boolean registered = false;
		// 计数先于一切初始化：构造中途失败（finally 归还）不留僵尸计数。
		liveInstances.incrementAndGet();
		try {
			this.zeze = zeze;
			RegisterZezeTables(zeze);
			registered = true;
			producer = new TransactionMQProducer(producerGroup);
			ClientConfigs.copyRoutingIdentity(clientConfig, producer);
			producer.setTransactionListener(this);
			// 自建回查线程池保留引用：destroyTransactionEnv 只对它 shutdown() 不等待，stop 需自行有界排空。
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
				// 表注册回滚（构造侧半边，与stop侧反注册同一配对不变量）：注册之后、initialized
				// 之前的失败（如copyRoutingIdentity对问题clientConfig的异常）只归还计数的话，tSent
				// 残留Application注册表且半构造对象不可达——无人再为它调stop()/反注册，同一
				// Application重建必撞duplicate table（addTable表id查重），本app的事务消息能力
				// 不可恢复。反注册自身的失败只记日志，不得吞换正在传播的原始异常。
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
		producer.start();
		// 每日清理tSent过期行：COMMIT路径保留的事务行若不定期删除，表会无界增长。
		if (tSentCleanFuture == null)
			tSentCleanFuture = TaskSpec.ofAction(this::cleanExpiredTSent)
					.scheduleAtPeriodNow(3, 30, 24 * 60 * 60 * 1000);
	}

	/**
	 * 停止生产者（幂等，可重复调用）：全部停机动作只在首次调用执行（stopped CAS 抢占），
	 * 后续调用直接返回，保障 liveInstances 计数与生命周期严格配对。
	 *
	 * <p>本方法同时反注册 tSent（{@link AbstractProducer#UnRegisterZezeTables}，与构造器的
	 * {@link AbstractProducer#RegisterZezeTables} 成对）：表从 Application 移除并关闭，同一
	 * Application 上 stop 后可直接重建 Producer（构造器重新注册，不撞 duplicate table）。
	 * 重建实例须在 {@link Application#start()} 之前完成构造（框架规则：start 只打开当时
	 * 已注册的表，后注册的表不打开）。典型停机顺序 stop()→app.close() 不受影响。
	 */
	public void stop() {
		if (!stopped.compareAndSet(false, true))
			return;
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
		// 在飞回查排空后反注册并关闭 tSent（与构造器注册成对；removeTable 幂等，补调无害）。
		// 反注册包try/catch（构造侧回滚同族）：getDatabase对不存在的库名抛IllegalStateException等
		// 不得跳过下方计数递减——计数上漂瓦解多实例告警判据，与丢表同属配对不完整。
		try {
			UnRegisterZezeTables(zeze);
		} catch (Throwable t) {
			logger.error("RocketMQ.Producer stop UnRegisterZezeTables fail. producerGroup="
					+ producer.getProducerGroup(), t);
		}
		// 完全停止后递减活实例计数：告警面与生命周期配对。递减与全部停机动作同处
		// stopped CAS 抢占之内，重复 stop 计数只递减一次。
		liveInstances.decrementAndGet();
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
	 *
	 * <p>查无行也<b>恒不答 COMMIT</b>（钉住的决定，勿改）：查无行在多实例误部署等场景下意味着
	 * "行在别的实例上"（本应答 COMMIT），但也可能是"本地事务从未执行/已回滚/已清理"——答 COMMIT
	 * 会把真丢的事务误提交，违背"仅当事务成功才发送"，且比 UNKNOW-丢弃更隐蔽（错误投递无法回收）。
	 * 两难的收敛出口是部署契约（producerGroup 单实例，见类 javadoc）+ tSentKeepTime 下界，
	 * 不是放宽本方法的回答。
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
