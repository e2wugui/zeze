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
import org.jspecify.annotations.Nullable;

/**
 * RocketMQ 消费者封装：将并发消费消息包装进独立的 Zeze 存储过程执行（成功返回 0 即 CONSUME_SUCCESS，
 * 失败或异常映射 RECONSUME_LATER）。本类不内建去重表，消费幂等由业务侧通过唯一键 upsert 或自建去重表保证；
 * 顺序消费（MessageListenerOrderly）需通过 {@link #setMessageListener(MessageListener)} 自行处理。
 */
public class Consumer {
	private static final Logger logger = LogManager.getLogger(Consumer.class);

	// 停机时等待在飞消费过程完成，避免 stop() 后关闭应用时访问已关闭的 Zeze 表
	private static final long STOP_AWAIT_MILLIS = 10_000L;

	public final Application zeze;
	private final DefaultMQPushConsumer consumer;

	/**
	 * @param clientConfig 路由与身份字段会透传给内部 consumer（见 {@link ClientConfigs}）
	 */
	public Consumer(Application zeze, String consumerGroup, ClientConfig clientConfig) {
		this.zeze = zeze;
		consumer = new DefaultMQPushConsumer(consumerGroup);
		ClientConfigs.copyRoutingIdentity(clientConfig, consumer);
		consumer.setAwaitTerminationMillisWhenShutdown(STOP_AWAIT_MILLIS);
	}

	/**
	 * 透传注册原始监听器（无事务包装，适用于纯通知或顺序消费）；须在 start 之前调用。
	 */
	public void setMessageListener(MessageListener messageListener) {
		consumer.setMessageListener(messageListener);
	}

	/**
	 * 注册事务型并发消息监听器：每条消息在独立 Zeze 过程中执行，返回 0 确认消费，非 0 或异常整批重投；须在 start 之前调用。
	 */
	public void setMessageListener(Func1<MessageExt, Long> handler) {
		consumer.setMessageListener(wrapTransactional(handler));
	}

	/**
	 * 将业务处理器包装为并发监听器：每条消息同步在 Zeze 过程中执行，结果码 0 映射 CONSUME_SUCCESS，失败或异常映射 RECONSUME_LATER。
	 */
	public MessageListenerConcurrently wrapTransactional(Func1<MessageExt, Long> handler) {
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
	 * 订阅消息，须在 start 之前调用。
	 */
	public void subscribe(String topic, @Nullable String subExpression) throws MQClientException {
		consumer.subscribe(topic, subExpression);
	}

	public void start() throws MQClientException {
		consumer.start();
	}

	/**
	 * 停止消费者并有界等待在飞消费过程结束，再由调用方关闭 Zeze 应用。
	 */
	public void stop() {
		consumer.shutdown();
	}

	public DefaultMQPushConsumer getConsumer() {
		return consumer;
	}
}
