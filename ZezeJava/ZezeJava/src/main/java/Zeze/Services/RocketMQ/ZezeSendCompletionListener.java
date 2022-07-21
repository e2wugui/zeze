package Zeze.Services.RocketMQ;

import javax.jms.CompletionListener;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

public class ZezeSendCompletionListener implements SendCallback {

	private CompletionListener completionListener;

	public ZezeSendCompletionListener(CompletionListener completionListener) {
		this.completionListener = completionListener;
	}

	@Override
	public void onSuccess(SendResult sendResult) {
		// TODO: how to transmit message into
		this.completionListener.onCompletion(null);
	}

	@Override
	public void onException(Throwable e) {
		// TODO: how to transmit message into
		this.completionListener.onException(null, new Exception(e));
	}
}
