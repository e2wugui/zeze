package Zeze.Services.RocketMQ;

import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
import Zeze.Util.FuncLong;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;

public class Producer extends AbstractProducer {
//	private static final Logger logger = LogManager.getLogger();
	public final Zeze.Application zeze;
	private TransactionMQProducer producer;


	public Producer(Zeze.Application zeze) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
	}

	public void start(String producerGroup, ClientConfig clientConfig) throws MQClientException {
		producer = new TransactionMQProducer(producerGroup);
		producer.setNamesrvAddr(clientConfig.getNamesrvAddr());
		producer.setTransactionListener(new MQListener());
		producer.start();
	}

	public void stop() {
		if (null != producer)
			producer.shutdown();
	}

	/**
	 * 发送消息，并且把消息跟一个事务绑定起来。仅当事务执行成功时，消息才会被发送。如果事务回滚，消息将被取消。
	 * @param msg message
	 * @param procedureAction procedure action
	 * @throws MQClientException send exception
	 */
	public void sendMessageWithTransaction(Message msg, FuncLong procedureAction) throws MQClientException {
		producer.sendMessageInTransaction(msg, procedureAction);
	}

	/*
	 * 发送消息，并且把消息跟一个本地存储绑定起来。仅当本地落地成功，消息才会被发送。如果落地失败，消息将被取消。
	 * @param msg message
	 * @throws MQClientException send exception
	 */
	/*
	public void sendMessageSafe(Message msg) throws MQClientException {
		producer.sendMessageInTransaction(msg, safeMessageAction);
	}

	private final SafeMessageAction safeMessageAction = new SafeMessageAction();
	static class SafeMessageAction {

		public boolean run(Message msg) throws Exception {
			return true;
		}
	}
	*/

	/**
	 * 发送普通消息。没有相关事务。
	 * @param msg message
	 * @throws MQBrokerException send exception
	 * @throws RemotingException send exception
	 * @throws InterruptedException send exception
	 * @throws MQClientException send exception
	 */
	public void sendMessage(Message msg) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
		producer.send(msg);
	}

	public TransactionMQProducer getProducer() {
		return producer;
	}

	class MQListener implements org.apache.rocketmq.client.producer.TransactionListener {
		@Override
		public LocalTransactionState executeLocalTransaction(Message msg, Object procedureObj) {
			/*
			if (procedureObj == safeMessageAction) {
				var safe = (SafeMessageAction)procedureObj;
				try {
					return safe.run(msg)
							? LocalTransactionState.COMMIT_MESSAGE
							: LocalTransactionState.ROLLBACK_MESSAGE;
				} catch (Exception e) {
					logger.error("", e);
					return LocalTransactionState.ROLLBACK_MESSAGE;
				}
			}
			*/
			var procedureAction = (FuncLong)procedureObj;
			var ret = Task.call(zeze.newProcedure(() -> {
				String transactionId = msg.getTransactionId();
				var result = new BTransactionMessageResult();
				result.setResult(true);
				result.setTimestamp(System.currentTimeMillis());
				_tSent.insert(transactionId, result);
				return procedureAction.call();
			}, "RocketMQ.LocalTransaction"));
			return ret == 0 ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
		}

		@Override
		public LocalTransactionState checkLocalTransaction(MessageExt msg) {
			var checkResult = new OutInt(0);
			try {
				zeze.newProcedure(() -> {
					String transactionId = msg.getTransactionId();
					var result = _tSent.get(transactionId);
					if (null != result && result.isResult())
						checkResult.value = 1;
					return 0;
				}, "checkLocalTransaction").call();
			} catch (Throwable ignored) { // ignored
			}
			return checkResult.value == 1 ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}
}
