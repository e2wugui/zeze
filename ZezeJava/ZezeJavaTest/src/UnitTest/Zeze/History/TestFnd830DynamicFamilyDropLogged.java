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
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-30回归（折中方案b）：dynamic集合的logTypeId不含值工厂身份——
 * 同(keyClass,DynamicBean)的第二个家族与首个同typeId，Helper注册端的
 * map2Dynamic/sortedMap2Dynamic原computeIfAbsent静默丢弃后续家族，回放端
 * Log.register先到先得，后家族日志用别人的create工厂解码（显式Bean:id编号
 * 重叠时静默解出错误bean）。修复：静默丢弃改为warn留痕（含两个宿主bean类名
 * 与变量名），同工厂重复注册不误报。
 * （方案a——比对每变量specialTypeId→beanClass映射表做启动error——需生成器
 * 把RealBeans暴露到产物，已记档上报，不在本补丁。）
 * 纯单元：直接驱动dependsMap/dependsSortedMap，伪造带newDynamicBean_Xxx
 * 静态方法的宿主类（与生成代码同形态）。
 */
@Fast
public class TestFnd830DynamicFamilyDropLogged {

	/** 家族1宿主：map<string,dynamic>变量managers（生成newDynamicBean_Xxx同形态）。 */
	public static final class HostA {
		public static DynamicBean newDynamicBean_Managers() {
			return new DynamicBean(1, b -> 1L, id -> new EmptyBean());
		}
	}

	/** 家族2宿主：同keyClass的另一家族。 */
	public static final class HostB {
		public static DynamicBean newDynamicBean_Items() {
			return new DynamicBean(2, b -> 2L, id -> new EmptyBean());
		}
	}

	static final class CapturingAppender extends AbstractAppender {
		final List<LogEvent> events = new ArrayList<>();

		CapturingAppender() {
			super("a3Fnd830Capture", null, null, true, Property.EMPTY_ARRAY);
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

	@BeforeEach
	public void setUp() {
		helperLogger = (Logger)LogManager.getLogger(Helper.class);
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
	}

	private static BVariable.Data var(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("map");
		v.setKey("string");
		v.setValue("dynamic");
		return v;
	}

	// 同(keyClass,DynamicBean)的第二个家族被丢弃时必须warn留痕（原实现静默），
	// 且保留先注册家族（Log.register先到先得的注册语义不变）。
	@Test
	public void testMapSecondFamilyDropLogged() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsMap(HostA.class, var("managers"), "string", "dynamic", result);
		Helper.dependsMap(HostB.class, var("items"), "string", "dynamic", result);

		assertEquals(1, result.map2Dynamic.size(), "同typeId只保留先注册家族（注册语义不变）");
		var kept = result.map2Dynamic.values().iterator().next();
		assertEquals(HostA.class.getName() + "#managers", kept.where, "保留的是先注册家族");
		assertTrue(appender.hasWarnContaining("keep=" + HostA.class.getName() + "#managers"
						+ " drop=" + HostB.class.getName() + "#items"),
				"丢弃第二个家族必须warn留痕（含两个宿主与变量名）");

		// 同一变量重复注册（相同工厂实例）：不告警。
		Helper.dependsMap(HostA.class, var("managers"), "string", "dynamic", result);
		assertFalse(appender.hasWarnContaining("drop=" + HostA.class.getName() + "#managers"),
				"相同工厂的重复注册不得误报");
	}

	// sortedmap同款。
	@Test
	public void testSortedMapSecondFamilyDropLogged() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsSortedMap(HostA.class, var("managers"), "long", "dynamic", result);
		Helper.dependsSortedMap(HostB.class, var("items"), "long", "dynamic", result);

		assertEquals(1, result.sortedMap2Dynamic.size());
		assertTrue(appender.hasWarnContaining("keep=" + HostA.class.getName() + "#managers"
				+ " drop=" + HostB.class.getName() + "#items"), "sortedmap丢弃第二家族必须warn");
	}

	// 不同keyClass的家族互不影响：都不丢弃、不告警。
	@Test
	public void testDifferentKeyClassNoDrop() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsMap(HostA.class, var("managers"), "string", "dynamic", result);
		Helper.dependsMap(HostB.class, var("items"), "long", "dynamic", result);

		assertEquals(2, result.map2Dynamic.size(), "不同keyClass不同typeId，都保留");
		assertFalse(appender.hasWarnContaining("dynamic collection family dropped"), "不得误报");
	}
}
