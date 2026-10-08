package Zeze.Services.Log4jQuery;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.LogService.BCondition;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;

import harness.Fast;

/**
 * 非法regex pattern的参数级分诊回归：pattern是终端用户可控自由文本（HTTP查询
 * 原样透传），写错正则（少右括号等）是日常输入错误。validateArgument若不预编译
 * 校验，PatternSyntaxException在会话锁内的searchRegex/browseRegex抛出，被框架
 * 统一翻成Procedure.Exception——客户端Session.checked只对LogicError与
 * INVALID_ARGUMENT分诊，其余按通用RuntimeException归瞬时失败：SessionAll不进
 * deadMembers不标finished，每次operate重发、服务端每次重抛，该成员查询无限
 * 重试失败且无人报参数错误。入口单点预编译（与查询路径同flags）拒绝以
 * INVALID_ARGUMENT，客户端InvalidArgumentException分诊（不拆会话、向调用方
 * 报参数错误）即刻生效。words非空时pattern被handler路由忽略，不校验。
 */
@Fast
@Extra
public class TestInvalidPatternRejectedAsInvalidArgument {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	/** 未闭合的"["（日常输入错误形态）：search入口参数级拒绝。 */
	@Test
	public void testIllegalSearchPatternRejected() throws Exception {
		assertEquals(Zeze.Services.LogService.INVALID_ARGUMENT,
				validateArgument(condition("["), null),
				"非法pattern必须在入口参数级拒绝（修复前返回Success，锁内compile抛出"
						+ "被翻成Procedure.Exception，客户端按瞬时失败无限重试）");
	}

	/** browse入口（带offsetFactor）同型：非法pattern参数级拒绝。 */
	@Test
	public void testIllegalBrowsePatternRejected() throws Exception {
		assertEquals(Zeze.Services.LogService.INVALID_ARGUMENT,
				validateArgument(condition("[unclosed"), 0.5f),
				"browse入口对非法pattern同样参数级拒绝");
	}

	/** 合法pattern不得误伤（与查询路径同flags编译）。 */
	@Test
	public void testValidPatternAccepted() throws Exception {
		assertEquals(Procedure.Success, validateArgument(condition("msg-\\d+.*"), null),
				"合法pattern照常通过");
		assertEquals(Procedure.Success, validateArgument(condition("(?i)ERROR.*"), 0f),
				"合法带标志pattern照常通过");
	}

	/** words非空时pattern被handler路由忽略（走contains路径无compile）：不得收紧现有可用请求。 */
	@Test
	public void testWordsRouteSkipsPatternCheck() throws Exception {
		var condition = condition("[");
		condition.getWords().add("any-word");
		assertEquals(Procedure.Success, validateArgument(condition, null),
				"words路由下pattern被忽略，非法pattern不得拒绝当前可用的contains查询");
	}

	private static BCondition.Data condition(String pattern) {
		var condition = new BCondition.Data();
		condition.setContainsType(BCondition.ContainsAll);
		condition.setPattern(pattern);
		return condition;
	}

	private static long validateArgument(BCondition.Data condition, Float offsetFactor) throws Exception {
		Method method = Zeze.Services.LogService.class.getDeclaredMethod(
				"validateArgument", BCondition.Data.class, Float.class);
		method.setAccessible(true);
		return (Long)method.invoke(null, condition, offsetFactor);
	}
}
