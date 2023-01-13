package Zeze.Services.RocketMQ;

import javax.ejb.Local;
import Zeze.Transaction.Transaction;
import Zeze.Util.OutInt;
import Zeze.Util.TaskCompletionSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

public class Producer extends AbstractProducer implements org.apache.rocketmq.client.producer.TransactionListener {

	private static final Logger logger = LogManager.getLogger(Producer.class);
	public Zeze.Application Zeze;
	private TransactionMQProducer producer;

	public Producer(Zeze.Application zeze) {
		Zeze = zeze;
		RegisterZezeTables(zeze);
	}

	public void start() {
		// todo 这里创建 producer，并建立连接。
		producer.setTransactionListener(this);
	}

	public void stop() {
		// todo 停止 producer.
	}

	public void sendMessageInTransaction(Message msg) throws MQClientException {
		String transactionId = msg.getTransactionId();
		var result = _tSent.getOrAdd(transactionId);
		result.setResult(true);
		result.setTimestamp(System.currentTimeMillis());

		var future = new TaskCompletionSource<Boolean>();
		Transaction.whileCommit(() -> future.setResult(true));
		Transaction.whileRollback(() -> future.setResult(false));
		producer.sendMessageInTransaction(msg, future);
	}

	@Override
	public LocalTransactionState executeLocalTransaction(Message msg, Object futureObj) {
		try {
			// 警告求助！
			// 这个转换有个警告，我点击了一下它的提示，这里就不警告了，但是build会有警告，我就加上，然后就这样了。
			@SuppressWarnings("unchecked")
			var future = (TaskCompletionSource<Boolean>)futureObj;
			return future.get() ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
		} catch (Throwable ex) {
			logger.error("", ex);
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
	}

	@Override
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		var checkResult = new OutInt(0);
		try {
			Zeze.newProcedure(() -> {
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
