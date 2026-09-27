package UnitTest.Zeze.History;

import java.util.ArrayList;
import java.util.List;

import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.History.Helper;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GC-C02 回归：dynamic List2 家族（list[dynamic:...] 值）冲突零信号。
 * 三层防线在 list dynamic 上全部失效：（1）Helper 层——FND8-30 的 putDynamicFamily
 * warn 只覆盖 map2/sortedMap2/gtable，dependsList 的 dynamic 分支直 add HashSet
 * （KV 值 equals 但工厂 lambda/method-ref 按对象身份比较，去重不生效）且零告警；
 * （2）typeId 层——List2Meta 的 dynamic 构造器 logTypeId 是全局固定单值
 * （Meta1.dynamicBeanTypeId，连 keyClass 分桶都没有），任意两个 dynamic list 变量
 * 必然同 typeId；（3）Log.register 层——meta.name 固定 "LogList2:DynamicBean"，
 * 同名分支只 debug。后注册家族被静默丢弃，回放端按先注册家族的工厂解码（显式
 * Bean:id 编号重叠时静默解出错误 bean）——比 GC-D02(FND19) 裁决保留的 map 家族
 * warn 底线更退一步（连 warn 都没有）。
 * 修复（域内最小对齐，复用 FND8-30 形态）：dependsList 的 dynamic 分支改走
 * putDynamicFamily——list 无 key 维度，键用固定哨兵（全部家族同键＝任意两个都
 * 冲突的语义）；先到家族保留、后到不同工厂 warn 留痕后丢弃（Log.register 同
 * typeId 先到先得，丢弃不改注册终态，只补检测面）。
 * typeId 固定单值/meta.name 固定串的彻底闭合（warn→error）依赖 Gen 侧
 * specialTypeId 映射暴露（与 GC-D02(FND19) 既定跟进同轨）及 List2Meta（八单元
 * 审计域外）——均只记录不越域，见 GC-R1报告。
 * 纯单元（对齐 TestFnd830DynamicFamilyDropLogged 形态）：直驱 dependsList，
 * 伪造带 newDynamicBean_Xxx 静态方法的宿主类（与生成代码同形态）。
 */
@Fast
public class TestFnd22GcC02ListDynamicFamilyWarn {

	/** 家族1宿主：list[dynamic]变量members（生成newDynamicBean_Xxx同形态）。 */
	public static final class HostA {
		public static DynamicBean newDynamicBean_Members() {
			return new DynamicBean(1, b -> 1L, id -> new EmptyBean());
		}
	}

	/** 家族2宿主：另一变量的dynamic list——typeId 无条件与家族1相同（Meta1.dynamicBeanTypeId）。 */
	public static final class HostB {
		public static DynamicBean newDynamicBean_Steps() {
			return new DynamicBean(2, b -> 2L, id -> new EmptyBean());
		}
	}

	static final class CapturingAppender extends AbstractAppender {
		final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("aFnd22GcC02Capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(@NotNull LogEvent event) {
			synchronized (events) {
				events.add(event.toImmutable());
			}
		}

		boolean hasWarnContaining(@NotNull String fragment) {
			synchronized (events) {
				return events.stream().anyMatch(e ->
						e.getLevel() == Level.WARN && e.getMessage().getFormattedMessage().contains(fragment));
			}
		}
	}

	private Logger helperLogger;
	private CapturingAppender appender;
	private Level prevLevel;

	@BeforeEach
	public void setUp() {
		helperLogger = (Logger)LogManager.getLogger(Helper.class);
		// tmp-build隔离车道无测试资源目录的log4j配置，缺省级别ERROR会把WARN在logger层滤掉
		//（gradle门禁有log4j2配置不受影响）。core.Logger.setLevel不传播到已创建logger的
		// privateConfig快照——必须走Configurator.setLevel（2.26标准API），事后还原。
		prevLevel = helperLogger.getLevel();
		Configurator.setLevel(Helper.class, Level.WARN);
		appender = new CapturingAppender();
		appender.start();
		helperLogger.addAppender(appender);
	}

	@AfterEach
	public void tearDown() {
		if (helperLogger != null && appender != null)
			helperLogger.removeAppender(appender);
		if (appender != null)
			appender.stop();
		if (helperLogger != null)
			Configurator.setLevel(Helper.class, prevLevel); // null=还原为继承
	}

	private static BVariable.Data var(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("list");
		v.setValue("dynamic");
		return v;
	}

	/**
	 * 第二个 dynamic list 家族（不同宿主/不同工厂）必须 warn 留痕且只保留先注册家族——
	 * 修复前 list2Dynamic 是 HashSet 且分支零告警：冲突三层零信号（本案核心）。
	 */
	@Test
	public void testListSecondFamilyDropLogged() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsList(HostA.class, var("members"), "dynamic", result);
		Helper.dependsList(HostB.class, var("steps"), "dynamic", result);

		assertEquals(1, result.list2Dynamic.size(), "全部dynamic list家族同typeId（固定单值），只保留先注册家族");
		var kept = result.list2Dynamic.values().iterator().next();
		assertEquals(HostA.class.getName() + "#members", kept.where, "保留的是先注册家族");
		assertTrue(appender.hasWarnContaining("keep=" + HostA.class.getName() + "#members"
						+ " drop=" + HostB.class.getName() + "#steps"),
				"第二个dynamic list家族必须warn留痕（含两个宿主与变量名）——修复前零信号");
	}

	/** 同一变量重复登记（相同工厂——非捕获lambda复用实例）：不告警，不误报。 */
	@Test
	public void testSameFamilyRepeatNoFalseWarn() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsList(HostA.class, var("members"), "dynamic", result);
		Helper.dependsList(HostA.class, var("members"), "dynamic", result);

		assertEquals(1, result.list2Dynamic.size());
		assertFalse(appender.hasWarnContaining("drop=" + HostA.class.getName() + "#members"),
				"相同工厂的重复登记不得误报");
	}
}
