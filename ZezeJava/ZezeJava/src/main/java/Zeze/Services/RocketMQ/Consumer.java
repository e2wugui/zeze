package Zeze.Services.RocketMQ;

import Zeze.Application;
import Zeze.Util.Func1;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * RocketMQ 消费侧桥（薄包装+幂等指南，不建桥级存根表）。
 *
 * <p>与 {@link Producer} 的"消息与本地事务原子绑定"对偶，本桥只提供事务边界包装：
 * {@link #wrapTransactional(Func1)} 把 listener 收到的每条消息放进一个独立的 Zeze
 * 存储过程执行，过程成功（返回0）= CONSUME_SUCCESS，过程失败/异常 = RECONSUME_LATER
 * （broker 按消费重试策略退避重投）。RocketMQ consume 线程池 → Zeze 过程的线程边界
 * 由包装处理：任意线程可开过程，包装内同步 call，不引入线程迁移。</p>
 *
 * <p><b>消费幂等是使用者的责任</b>——rocketmq-client 是 at-least-once 投递：消费过程
 * 崩溃后重投、RECONSUME_LATER 重试、生产端重发都会造成同一条业务消息被处理多次。
 * 三种范式（按优先级）：</p>
 * <ol>
 * <li><b>业务唯一键 upsert（首选，"表消费"场景）</b>：消息携带业务键（如订单号），
 * 消费动作是把业务表行 upsert 成消息内容的最终值。重复消费天然收敛到同一结果，
 * 不需要任何额外机制。同 key 不同内容的消息按"后到为准"语义确认可接受即可。</li>
 * <li><b>自建去重表</b>：业务动作不天然幂等（如发号、外部副作用）时，在业务库自建
 * 一张以业务去重键为键的存根表，消费过程内（同一个 Zeze 过程，与业务写原子）先查
 * 存根、执行业务、写存根。</li>
 * <li><b>消费组+msgId 存根 DIY 模板</b>：没有业务键可用时的兜底。在过程内查/写
 * key = consumerGroup + ":" + msg.getMsgId() 的存根行（对偶 Producer 的 tSent 形态，
 * 按时间清理过期行）。示例骨架：
 * <pre>{@code
 * // tConsumed(consumerGroup:msgId -> timestamp)，随应用建表注册，自建清理任务按保留期删除
 * consumer.setMessageListener((Func1<MessageExt, Long>) msg ->
 *     Zeze.App.Instance.Zeze.newProcedure(() -> {
 *         if (tConsumed.get(group + ":" + msg.getMsgId()) != null)
 *             return 0;                       // 已消费：幂等成功
 *         doBusiness(msg);                    // 业务动作（同过程内）
 *         tConsumed.insert(group + ":" + msg.getMsgId(), System.currentTimeMillis());
 *         return 0;
 *     }, "consume-" + topic).call());
 * }</pre></li>
 * </ol>
 *
 * <p><b>机制级去重只挡投递重复，挡不住业务重发</b>：msgId 在 broker 侧重投时稳定，但
 * producer 侧重发（业务层重试）会产生新 msgId/UNIQ_KEY——范式 3 对此无效，范式 1/2
 * 的业务键才能收敛。这正是本桥不内建存根表的原因（零表开销；等第一个真实消费者出现
 * 再评估要不要进桥）。毒消息/退避上限/死信语义宜与 Zeze 自家 MQ 的
 * 消费失败语义统一设计，本桥不先行实现。</p>
 *
 * <p>包装只覆盖并发消费（MessageListenerConcurrently，默认形态）；顺序消费
 * （MessageListenerOrderly）的失败语义是本地挂起而非重投，不适用本包装，用
 * {@link #setMessageListener(MessageListener)} 透传自行处理。</p>
 */
public class Consumer {
	private static final Logger logger = LogManager.getLogger(Consumer.class);

	// stop 的有界排空预算：consume 线程池在飞的 wrapTransactional 过程（含冲突重试）须在
	// shutdown 内完成——典型停机顺序 stop()→app.close()，越过即对已关 Zeze 表的访问。
	private static final long STOP_AWAIT_MILLIS = 10_000L;

	public final @NotNull Application zeze;
	private final @NotNull DefaultMQPushConsumer consumer;

	/**
	 * @param clientConfig 传入即生效：namesrvAddr/namespace/instanceName 等路由/身份字段透传给
	 *                     内部 consumer（见 {@link ClientConfigs}），未列字段可经 {@link #getConsumer()} 设置。
	 */
	public Consumer(@NotNull Application zeze, @NotNull String consumerGroup, @NotNull ClientConfig clientConfig) {
		this.zeze = zeze;
		consumer = new DefaultMQPushConsumer(consumerGroup);
		ClientConfigs.copyRoutingIdentity(clientConfig, consumer);
		consumer.setAwaitTerminationMillisWhenShutdown(STOP_AWAIT_MILLIS);
	}

	/**
	 * 透传注册原始 listener（可选性出口：纯通知类消费不需要事务边界；顺序消费自行处理）。
	 * 需要在start之前注册监听器。
	 */
	public void setMessageListener(@NotNull MessageListener messageListener) {
		consumer.setMessageListener(messageListener);
	}

	/**
	 * 注册"每消息一个 Zeze 过程"的监听器（薄包装）：
	 * handler 在存储过程内执行，返回 0 = CONSUME_SUCCESS；非 0 或抛异常 = RECONSUME_LATER。
	 * 批量投递（consumeMessageBatchMaxSize&gt;1）时逐条执行，任一失败即整批 RECONSUME_LATER，
	 * 已成功条目会随重投再次到达——幂等责任见类注释。
	 * 需要在start之前注册监听器。
	 */
	public void setMessageListener(@NotNull Func1<MessageExt, Long> handler) {
		consumer.setMessageListener(wrapTransactional(handler));
	}

	/**
	 * 事务边界包装：RocketMQ consume 线程 → Zeze 过程的桥。
	 * 每条消息独立过程（过程名 RocketMQ.Consumer.consumeMessage）：
	 * 成功（结果码0）映射 CONSUME_SUCCESS，过程失败（冲突重试耗尽等非0码）或
	 * 异常映射 RECONSUME_LATER，异常不向 rocketmq-client 泄漏（否则线程池吞掉且无重投语义）。
	 */
	public @NotNull MessageListenerConcurrently wrapTransactional(@NotNull Func1<MessageExt, Long> handler) {
		return (msgs, context) -> {
			for (var msg : msgs) {
				var rc = TaskSpec.ofProcedure(
						zeze.newProcedure(() -> handler.call(msg), "RocketMQ.Consumer.consumeMessage")).call();
				if (rc != 0) {
					logger.warn("consume message procedure fail, reconsume later: msgId={} rc={}", msg.getMsgId(), rc);
					return ConsumeConcurrentlyStatus.RECONSUME_LATER;
				}
			}
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		};
	}

	/**
	 * 订阅消息。
	 * 需要在start之前调用。
	 */
	public void subscribe(@NotNull String topic, @Nullable String subExpression) throws MQClientException {
		consumer.subscribe(topic, subExpression);
	}

	public void start() throws MQClientException {
		consumer.start();
	}

	/**
	 * 停止消费者：shutdown 内有界等待（awaitTerminationMillisWhenShutdown，见构造器）在飞消费任务
	 * （wrapTransactional 过程）完成，stop 返回后再关闭 Zeze 应用不与在飞消费过程竞态。
	 */
	public void stop() {
		consumer.shutdown();
	}

	public @NotNull DefaultMQPushConsumer getConsumer() {
		return consumer;
	}
}
