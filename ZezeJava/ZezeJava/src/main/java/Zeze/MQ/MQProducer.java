package Zeze.MQ;

import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;

/**
 * 用户接口
 *
 * 生产者
 */
public class MQProducer {
	private final MQ mq; // 这里直接保存MQ引用，并且内部不管理，因为一个进程很少对同一个队列打开多个生产者消费者。

	public MQProducer(String topic) {
		mq = MQ.openMQ(topic);
	}

	public void sendMessage(Object key, BMessage.Data message) {
		mq.sendMessage(hash(key.hashCode()), message);
	}

	public void sendMessage(int key, BMessage.Data message) {
		mq.sendMessage(hash(key), message);
	}

	public void sendMessage(long key, BMessage.Data message) {
		mq.sendMessage(hash(Long.hashCode(key)), message);
	}

	private static int hash(int _h) {
		int h = _h;
		h ^= (h >>> 20) ^ (h >>> 12);
		return (h ^ (h >>> 7) ^ (h >>> 4));
	}
	public void close() {
		mq.close();
	}
}
