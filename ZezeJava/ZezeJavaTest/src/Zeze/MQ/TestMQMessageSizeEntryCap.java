package Zeze.MQ;

import java.nio.file.Path;
import java.util.HashMap;
import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BSendMessage;
import Zeze.Builtin.MQ.SendMessage;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SendMessage 入口字节上界回归：超过 MQConfig.MaxMessageBytes（默认 16MB）的消息在
 * ProcessSendMessageRequest 入口被响亮拒绝（eMessageTooLarge），不进入内存队列——协议层
 * ProxyServer 放行 100MB，装载治理只按条数（4096）时，大消息积压/重启装载按
 * "协议上限×条数"承诺内存，Manager 确定性 OOM。
 * <p>
 * 直驱 handler（同包 protected 缝）：拒绝路径在 SendResult 之前返回，无需网络；拒绝先于
 * topic/分区校验（over-size 对不存在 topic 也必须报 eMessageTooLarge 而非 eTopicNotExist）。
 * 边界值（编码尺寸恰等于上界）必须通过尺寸检查（判 eTopicNotExist，因 topic 不存在）。
 */
@Fast
public class TestMQMessageSizeEntryCap {

	private static final int MaxMessageBytesDefault = 16 * 1024 * 1024;

	private static long combined(int code) {
		return ((long)AbstractMQManager.ModuleId << 32) | code;
	}

	private static BMessage.Data messageOf(int bodySize) {
		return new BMessage.Data(0, new HashMap<>(), new Binary(new byte[bodySize]));
	}

	// 与 MQSingle.messageBytes 同尺（编码尺寸；allocate 按 body 预留避免增长复制）。
	private static long encodedBytes(BMessage.Data message) {
		var bb = ByteBuffer.Allocate(64 + message.getBody().size());
		message.encode(bb);
		return bb.size();
	}

	@Test
	public void testOversizeRejectedLoudlyAtEntry(@TempDir Path tempDir) throws Exception {
		var manager = new MQManager(tempDir.resolve("manager").toString(), new Config());
		try {
			Assertions.assertEquals(MaxMessageBytesDefault, manager.getMqConfig().getMaxMessageBytes(),
					"默认上界 16MB（部署可经 MQConfig.MaxMessageBytes 调整）");

			var oversize = messageOf(MaxMessageBytesDefault); // body 即超限
			Assertions.assertTrue(encodedBytes(oversize) > MaxMessageBytesDefault);
			var r = new SendMessage();
			r.Argument = new BSendMessage.Data("no-such-topic", 0, oversize);
			Assertions.assertEquals(combined(10), manager.ProcessSendMessageRequest(r),
					"超限消息必须在入口被 eMessageTooLarge(10) 拒绝（先于 topic 校验）");

			// 边界值：编码尺寸恰等于上界——必须通过尺寸检查（到达 topic 校验报 eTopicNotExist）。
			// 编码=字段tag+varint长度+body，先粗定位再逐字节推进到恰等于上界（16MB 落在 4 字节
			// varint 档内，编码随 body 单调 +1，精确命中可保证）。
			var body = MaxMessageBytesDefault - 12;
			while (encodedBytes(messageOf(body)) < MaxMessageBytesDefault)
				++body;
			var boundary = messageOf(body);
			Assertions.assertEquals(MaxMessageBytesDefault, encodedBytes(boundary), "边界消息编码尺寸恰等于上界");
			var rb = new SendMessage();
			rb.Argument = new BSendMessage.Data("no-such-topic", 0, boundary);
			Assertions.assertEquals(combined(2), manager.ProcessSendMessageRequest(rb),
					"边界值必须通过尺寸检查（后续 eTopicNotExist 来自 topic 不存在，非尺寸拒绝）");
		} finally {
			manager.stop();
		}
	}
}
