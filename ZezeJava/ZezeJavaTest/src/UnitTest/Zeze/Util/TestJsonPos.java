package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Json.Pos 语义回归：Pos 是"输入偏移书签"——解析时声明为 Pos 的字段/元素
 * 不解析值本身，只记录该值在原始输入中的起始字节偏移；写出时 Pos 字段被跳过，
 * Pos 作为值直接输出时写成普通数字。
 * 偏移是【字节】偏移（UTF-8），不是字符下标。
 */
@Fast
public class TestJsonPos {
	public static class Chat { // 消息头：body 只在需要时才回头解析
		public String cmd;
		public Json.Pos body; // 不解析 body 的值，只记住它从哪开始
		public int seq;
	}

	public static class ChatBody { // body 的真实结构，第二遍解析用
		public String text;
		public List<Integer> to;
	}

	public static class TwoPos { // 验证一个类可以有多个 Pos 字段，各自独立记录
		public Json.Pos first;
		public int mid;
		public Json.Pos last;
	}

	public static class Batch {
		public List<Json.Pos> offsets = new ArrayList<>(); // 数组每个元素的位置
	}

	@Test
	public void testPosFieldRecordsValueStart() throws Exception {
		var bodyJson = "{\"text\":\"hi\",\"to\":[1,2]}";
		// 前缀含多字节UTF-8字符，确认记录的是字节偏移而非字符下标
		var input = "{\"cmd\":\"聊天\",\"seq\":7,\"body\":" + bodyJson + "}";
		var bytes = input.getBytes(StandardCharsets.UTF_8);

		var chat = new JsonReader(bytes).parse(new Chat());

		assertEquals("聊天", chat.cmd); // 其余字段照常解析
		assertEquals(7, chat.seq);
		// body 的值一个字节都没被消费，偏移恰好指向它的第一个字节 '{'
		assertEquals(bodyJson, new String(bytes, chat.body.pos,
				bodyJson.getBytes(StandardCharsets.UTF_8).length, StandardCharsets.UTF_8));
	}

	@Test
	public void testSecondPassParseFromPos() throws Exception {
		var bodyJson = "{\"text\":\"hi\",\"to\":[1,2]}";
		var bytes = ("{\"cmd\":\"chat\",\"seq\":7,\"body\":" + bodyJson + "}").getBytes(StandardCharsets.UTF_8);

		var jr = new JsonReader(bytes);
		var chat = jr.parse(new Chat());
		assertEquals("chat", chat.cmd);
		assertEquals(7, chat.seq);

		// 游标拨回 body 的偏移，从那里按真实结构二次解析
		jr.pos(chat.body.pos);
		var body = jr.parse(new ChatBody());
		assertEquals("hi", body.text);
		assertEquals(List.of(1, 2), body.to);
	}

	@Test
	public void testMultiplePosFieldsAreIndependent() throws Exception {
		var input = "{\"first\":[1,2],\"mid\":9,\"last\":\"xy\"}";
		var bytes = input.getBytes(StandardCharsets.UTF_8);

		var two = new JsonReader(bytes).parse(new TwoPos());

		assertEquals(9, two.mid);
		// 两个 Pos 字段各自记录各自值的起始字节：first 指向 '['，last 指向 '"'
		assertEquals('[', (char)bytes[two.first.pos]);
		assertEquals('"', (char)bytes[two.last.pos]);
		// 从各自偏移出发都能二次解析出正确的值
		// 无类型parseArray按parseNumber的规则返回Integer/Long（elemClass版本是给bean用的，数字会被当bean原样返回）
		var secondPass = new ArrayList<>(); // parseArray就地填充
		new JsonReader(bytes).pos(two.first.pos).parseArray(secondPass);
		assertEquals(2, secondPass.size());
		assertEquals(1L, ((Number)secondPass.get(0)).longValue());
		assertEquals(2L, ((Number)secondPass.get(1)).longValue());
	}

	@Test
	public void testListPosRecordsElementOffsets() throws Exception {
		var input = "{\"offsets\":[10,\"abc\",true]}";
		var bytes = input.getBytes(StandardCharsets.UTF_8);

		var batch = new JsonReader(bytes).parse(new Batch());

		assertEquals(3, batch.offsets.size());
		// 每个元素记录的是它自己的起始字节，而不是元素值
		assertEquals('1', (char)bytes[batch.offsets.get(0).pos]);
		assertEquals('"', (char)bytes[batch.offsets.get(1).pos]);
		assertEquals('t', (char)bytes[batch.offsets.get(2).pos]);
	}

	@Test
	public void testWriteSkipsPosFieldAndWritesPosValueAsNumber() throws Exception {
		var bytes = "{\"cmd\":\"chat\",\"seq\":7,\"body\":{\"text\":\"hi\"}}".getBytes(StandardCharsets.UTF_8);
		var chat = new JsonReader(bytes).parse(new Chat());

		// Pos 字段是内部记账状态，序列化时被跳过
		assertEquals("{\"cmd\":\"chat\",\"seq\":7}", new String(new JsonWriter().write(chat).toBytes()));

		// Pos 作为值直接输出时写成它的 int
		var offsets = new ArrayList<Json.Pos>();
		offsets.add(new Json.Pos(12));
		offsets.add(new Json.Pos(15));
		offsets.add(new Json.Pos(21));
		assertEquals("[12,15,21]", new String(new JsonWriter().write(offsets).toBytes()));
	}
}
