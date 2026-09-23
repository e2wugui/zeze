package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.TreeMap;
import Zeze.Util.JsonReader;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * FND11 util-01/util-02 回归：非字符串 map 键的解析契约。
 * util-01：无引号（JSON5 裸键）boolean 键从词首判定真值并消费整个词，pos 停在分隔符上
 * （parse*Key 家族约定，随后 skipColon→next() 从 buf[pos] 起读）——修复前 ++pos 越过词首
 * 读第二字节（true 读成 'r' 恒 false）且 pos 停在词中，键值双损坏；R1 增量审 I-02 又修正了
 * 词消费终点（判据取当前字符推到 ':' 上）并补词尾长度防护。
 * util-02：Character 键读写两侧数值口径对齐（写侧输出码点，读侧 parseInt+(char)）。
 */
@Fast
public class TestFnd11JsonBooleanCharKey {
	private static JsonReader jrOf(String s) {
		return new JsonReader(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	public void testUnquotedBooleanKeyValueIntact() {
		// {true:1}：键=TRUE，且词消费后 skipColon 正常越 ':'，值 '1' 可被 next() 读到
		var jr = jrOf("true:1");
		var key = JsonReader.parseBooleanKey(jr, 't');
		assertEquals(Boolean.TRUE, key);
		assertEquals(':', jr.next(), "pos 必须停在 ':' 上（家族约定）");
		assertEquals('1', jr.skipNext());
		assertEquals(1, jr.parseInt());

		// {false:2}：词首 'f' 判 false，同样消费整个词
		var jr2 = jrOf("false:2");
		var key2 = JsonReader.parseBooleanKey(jr2, 'f');
		assertEquals(Boolean.FALSE, key2);
		assertEquals(':', jr2.next());
		assertEquals('2', jr2.skipNext());
		assertEquals(2, jr2.parseInt());
	}

	@Test
	public void testUnquotedBooleanKeyWordAtBufferEndNoAIOOBE() {
		// 词尾恰为缓冲区末尾（顶层裸 true）：按 parseInt 同款长度防护安全终止
		var jr = jrOf("true");
		assertEquals(Boolean.TRUE, JsonReader.parseBooleanKey(jr, 't'));
	}

	@Test
	public void testQuotedCharacterKeyNumericCodepoint() {
		// util-02 读侧：带引号 Character 键按十进制码点解析（写侧已对齐输出码点数值）
		var jr = jrOf("\"65\":1");
		var key = JsonReader.parseCharKey(jr, '"');
		assertEquals(Character.valueOf('A'), key); // (char)65
		// 非数字字形键仍按既有口径抛 NumberFormatException（写侧不再产出这种形态）
		var jr2 = jrOf("\"A\":1");
		assertThrows(NumberFormatException.class, () -> JsonReader.parseCharKey(jr2, '"'));
	}

	@Test
	public void testCharacterMapRoundtripViaTreeMapApi() {
		// 写侧：Character 键输出码点数值（紧凑模式）——'A' 输出 65 而非字形 "A"
		var map = new TreeMap<Character, Integer>();
		map.put('A', 1);
		map.put('5', 2);
		var json = Zeze.Util.Json.toCompactString(map);
		assertEquals("{\"53\":2,\"65\":1}", json); // '5'=0x35=53, 'A'=0x41=65（TreeMap自然序）
	}
}
