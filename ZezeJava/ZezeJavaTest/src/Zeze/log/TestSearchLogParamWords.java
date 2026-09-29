package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import Zeze.log.handle.entity.SearchLogParam;

import harness.Fast;

/**
 * /api/search 与 /api/browse 关键词参数 words 的拆分语义直测
 * （FND30 zokermanager-03）：wordsToList 必须过滤空段并 trim——
 * 前导/连续逗号（"error,,warn"、",x"）产生的 "" 段若进入 BCondition.Words，
 * 服务器字面量包含判定 log.contains("") 恒真：ContainsAny 整体旁路返回全量、
 * ContainsNone 恒空。全空白/纯分隔符时收敛为空列表（服务器侧 words 空=不过滤，
 * 既有语义）。
 */
@Fast
public class TestSearchLogParamWords {

	/** 连续/前导逗号的空段不得进入结果（修复前 split(",") 原样保留 "" 段——本用例红）。 */
	@Test
	public void testEmptySegmentsFiltered() {
		assertEquals(List.of("error", "warn"), param("error,,warn").wordsToList(),
				"连续逗号：空段被过滤，不进 BCondition.Words");
		assertEquals(List.of("x"), param(",x").wordsToList(),
				"前导逗号：首段空串被过滤");
		assertEquals(List.of("x"), param("x,").wordsToList(),
				"尾部逗号：Java split 默认已去尾空段，语义保持");
		assertEquals(List.of("a"), param("a,,,").wordsToList(),
				"多段连续空段全部过滤");
	}

	/** 每段 trim 后再判空：段内空白视同空段，非空段去首尾空白进入结果。 */
	@Test
	public void testTrimEachSegment() {
		assertEquals(List.of("error", "warn"), param(" error , warn ").wordsToList(),
				"非空段去首尾空白");
		assertEquals(List.of("x"), param(" x ").wordsToList());
	}

	/** 整体空白/纯分隔符（trim 后无有效段）：收敛为空列表——服务器 words 空=不过滤。 */
	@Test
	public void testBlankWordsToEmptyList() {
		assertEquals(List.of(), param(" , , ").wordsToList(),
				"纯空白+逗号：无有效关键词，空列表（本用例红：修复前为 [\" \", \" \"]）");
		assertEquals(List.of(), param(",,").wordsToList());
		assertEquals(List.of(), param("   ").wordsToList());
	}

	/** 既有语义回归：null/空串返回空列表。 */
	@Test
	public void testNullAndEmptyWords() {
		assertEquals(List.of(), param(null).wordsToList());
		assertEquals(List.of(), param("").wordsToList());
		assertTrue(param(null).wordsToList().isEmpty());
	}

	private static SearchLogParam param(String words) {
		var p = new SearchLogParam();
		p.setWords(words);
		return p;
	}
}
