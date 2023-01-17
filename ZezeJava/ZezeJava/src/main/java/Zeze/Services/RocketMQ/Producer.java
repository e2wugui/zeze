package Zeze.Services.RocketMQ;

import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
import Zeze.Util.FuncLong;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;

public class Producer extends AbstractProducer {

	private static final Logger logger = LogManager.getLogger(Producer.class);
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

	public void sendMessageWithTransaction(Message msg, FuncLong procedureAction) throws MQClientException {
		producer.sendMessageInTransaction(msg, procedureAction);
	}

	public void sendMessage(Message msg) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
		producer.send(msg);
	}

	public TransactionMQProducer getProducer() {
		return producer;
	}

	class MQListener implements org.apache.rocketmq.client.producer.TransactionListener {
		@Override
		public LocalTransactionState executeLocalTransaction(Message msg, Object procedureObj) {
			@SuppressWarnings("unchecked")
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
			} catch (Throwable ignored) {
			}
			return checkResult.value == 1 ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}
}
