package Zeze.Services;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND24 zoker-12 回归：Zoker 模块错误码命名空间不得同值并账——历史上
 * eDuplicateZoker 与 eOpenError 同为 1，Register 应答与 OpenFile 应答的
 * resultCode 数值相同，按 (moduleId, code) 聚类的监控/报告把两类错误并账。
 * eDuplicateZoker 迁移到空闲值 11（1-10 已被同模块占用）。
 */
@Fast
public class TestZokerErrorCodeDistinct {

	@Test
	public void testDuplicateZokerNotAliasedToOpenError() {
		Assertions.assertNotEquals(AbstractZoker.eDuplicateZoker, AbstractZoker.eOpenError,
				"同模块错误码同值=按码聚类的消费方并账（修复前两者同为 1）");
		Assertions.assertTrue(AbstractZoker.eDuplicateZoker > 0, "错误码须为正");
	}
}
