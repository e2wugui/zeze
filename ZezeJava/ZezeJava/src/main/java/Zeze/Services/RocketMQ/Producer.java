package Zeze.Services.RocketMQ;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Builtin.RocketMQ.Producer.BTransactionMessageResult;
import Zeze.Util.FuncLong;
import Zeze.Util.Task;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.jetbrains.annotations.NotNull;

public class Producer extends AbstractProducer implements TransactionListener {
	public final @NotNull Application zeze;
	private final @NotNull TransactionMQProducer producer;

	public Producer(@NotNull Application zeze, @NotNull String producerGroup, @NotNull ClientConfig clientConfig) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
		producer = new TransactionMQProducer(producerGroup);
		producer.setNamesrvAddr(clientConfig.getNamesrvAddr()); // "127.0.0.1:9876"
		producer.setTransactionListener(this);
		producer.setExecutorService(new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000),
				r -> new Thread(r, "client-transaction-msg-check-thread")));
	}

	public void start() throws MQClientException {
		producer.start();
	}

	public void stop() {
		producer.shutdown();
	}

	public @NotNull TransactionMQProducer getProducer() {
		return producer;
	}

	/**
	 * 发送普通消息。没有相关事务。
	 */
	public void sendMessage(@NotNull Message msg) throws Exception {
		producer.send(msg);
	}

	/**
	 * 发送消息，并且把消息跟一个事务绑定起来。仅当事务执行成功时，消息才会被发送。如果事务回滚，消息将被取消。
	 */
	public void sendMessageWithTransaction(@NotNull Message msg,
										   @NotNull FuncLong procedureAction) throws MQClientException {
		// msg = new Message("Topic", "tag1 || tag2", "key1", "Message Body".getBytes(RemotingHelper.DEFAULT_CHARSET));
		var txnId = zeze.getAutoKey("RocketMQ").nextString();
		msg.setTransactionId(txnId);
		if (Task.call(zeze.newProcedure(() -> {
			_tSent.insert(txnId, new BTransactionMessageResult(false, System.currentTimeMillis()));
			return 0;
		}, "RocketMQ.executeLocalTransaction")) == 0)
			producer.sendMessageInTransaction(msg, procedureAction);
	}

	@Override
	public @NotNull LocalTransactionState executeLocalTransaction(@NotNull Message msg, Object arg) {
		if (Task.call(zeze.newProcedure(() -> {
			var sent = _tSent.get(msg.getTransactionId());
			if (sent == null)
				return 1;
			if (sent.isResult())
				return 0;
			sent.setResult(true);
			return ((FuncLong)arg).call();
		}, "RocketMQ.executeLocalTransaction")) == 0)
			return LocalTransactionState.COMMIT_MESSAGE;

		Task.call(zeze.newProcedure(() -> {
			_tSent.remove(msg.getTransactionId());
			return 0;
		}, "RocketMQ.executeLocalTransaction.rollback"));
		return LocalTransactionState.ROLLBACK_MESSAGE;
	}

	@Override
	public @NotNull LocalTransactionState checkLocalTransaction(@NotNull MessageExt msg) {
		var sent = _tSent.selectDirty(msg.getTransactionId());
		if (sent == null)
			return LocalTransactionState.ROLLBACK_MESSAGE;
		return sent.isResult() ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.UNKNOW;
	}
}
