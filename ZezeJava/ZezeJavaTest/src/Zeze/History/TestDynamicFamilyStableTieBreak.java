package Zeze.History;

import harness.Extra;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FND25 hist-01 回归：dynamic 家族同 typeId 冲突的胜者按家族来源名（宿主bean全名#变量名）
 * 字典序稳定决胜，与 depends 扫描的到达顺序无关——替代 putIfAbsent 的先到先得
 * （DependsResult 各容器的 Class identityHashCode 桶序跨 JVM/重启不稳定，先到先得的
 * 胜者可翻转：回放端解码工厂跨重启变化，Bean id 重叠静默解错 bean、不重叠毒卡游标）。
 * 既有 TestDynamicFamilyDropLogged 只覆盖"先到者字典序更小"（无翻转）的形态，本用例直驱
 * 翻转面：后到但字典序更小的家族必须替换占位者，且两种到达序收敛到同一终胜者。
 * 纯单元：直接驱动 dependsMap/dependsList，伪造带 newDynamicBean_Xxx 静态方法的宿主类
 * （与生成代码同形态）。
 */
@Fast
@Extra
public class TestDynamicFamilyStableTieBreak {

	/** 字典序较大的家族（先注册的占位者）。 */
	public static final class HostZulu {
		public static DynamicBean newDynamicBean_Items() {
			return new DynamicBean(2, b -> 2L, id -> new EmptyBean());
		}
	}

	/** 字典序较小的家族（后注册的挑战者，必须翻转胜出）。 */
	public static final class HostAlpha {
		public static DynamicBean newDynamicBean_Managers() {
			return new DynamicBean(1, b -> 1L, id -> new EmptyBean());
		}
	}

	private static BVariable.Data mapVar(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("map");
		v.setKey("string");
		v.setValue("dynamic");
		return v;
	}

	private static BVariable.Data listVar(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("list");
		v.setValue("dynamic");
		return v;
	}

	private static BVariable.Data gtableVar(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("gtable");
		v.setKey("string,string");
		v.setValue("dynamic");
		return v;
	}

	private static String whereOf(Class<?> host, String varName) {
		return host.getName() + '#' + varName;
	}

	// 后到但字典序更小的家族必须替换占位者（先到先得在此形态下会保留 HostZulu——
	// 跨 JVM 遍历序翻转即该形态，胜者不稳定）。
	@Test
	public void testLaterLexicographicallySmallerFamilyWins() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsMap(HostZulu.class, mapVar("items"), "string", "dynamic", result);
		assertEquals(1, result.map2Dynamic.size());
		Helper.dependsMap(HostAlpha.class, mapVar("managers"), "string", "dynamic", result);

		assertEquals(1, result.map2Dynamic.size(), "同key(typeId)冲突仍收敛到单一家族");
		assertEquals(whereOf(HostAlpha.class, "managers"),
				result.map2Dynamic.values().iterator().next().where,
				"胜者=where字典序最小者：后到更小者必须翻转替换占位者（与到达顺序无关）");
	}

	// 到达序无关性：反序注册收敛到同一终胜者（跨 JVM/重启恒定的契约本体）。
	@Test
	public void testWinnerIndependentOfArrivalOrder() throws Exception {
		var reversed = new Helper.DependsResult();
		Helper.dependsMap(HostAlpha.class, mapVar("managers"), "string", "dynamic", reversed);
		Helper.dependsMap(HostZulu.class, mapVar("items"), "string", "dynamic", reversed);

		assertEquals(whereOf(HostAlpha.class, "managers"),
				reversed.map2Dynamic.values().iterator().next().where,
				"反序到达的终胜者必须与正序相同（argmin(where) 与到达序无关）");
	}

	// list 家族（全局唯一哨兵键，任意两个 dynamic list 必冲突）同治：后到更小者胜。
	@Test
	public void testListFamilyStableTieBreak() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsList(HostZulu.class, listVar("items"), "dynamic", result);
		Helper.dependsList(HostAlpha.class, listVar("managers"), "dynamic", result);

		assertEquals(1, result.list2Dynamic.size(), "哨兵单键：全部 dynamic list 家族同一冲突域");
		assertEquals(whereOf(HostAlpha.class, "managers"),
				result.list2Dynamic.values().iterator().next().where,
				"list 家族胜者同样=where字典序最小者");
	}

	// gtable 外层 pmapMeta（FND26 H/M-1 + FND27 H1，外层收口）：同 (rowClass,colClass) 的
	// dynamic gtable 变量外层 typeId 相同而 valueCtor 各异——修复前走 map2Metas 的 HashSet
	// 身份序+Log.register 先到先得，胜者跨 JVM/重启翻转（回放解码工厂非确定）。修复后按
	// 来源名字典序决胜，后到更小者必须替换占位者。
	@Test
	public void testOuterGTableMetaStableTieBreak() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsGTable(HostZulu.class, gtableVar("items"), "string", "string", "dynamic", result);
		assertEquals(1, result.map2MetasDynamic.size());
		Helper.dependsGTable(HostAlpha.class, gtableVar("managers"), "string", "string", "dynamic", result);

		assertEquals(1, result.map2MetasDynamic.size(), "同(row,col)冲突仍收敛到单一外层meta");
		assertEquals(whereOf(HostAlpha.class, "managers"),
				result.map2MetasDynamic.values().iterator().next().where,
				"外层胜者=where字典序最小者：后到更小者必须翻转替换占位者（与到达顺序无关）");
		// 冲突键不串桶：不同(row,col)各自独立决胜（外层typeId按(row,col)分桶）。
		Helper.dependsGTable(HostAlpha.class, gtableVar("managers"), "string", "long", "dynamic", result);
		assertEquals(2, result.map2MetasDynamic.size(), "不同(row,col)键互不冲突");
	}

	// 外层meta到达序无关性（外层收口的契约本体）：反序注册收敛到同一终胜者。
	@Test
	public void testOuterGTableMetaWinnerIndependentOfArrivalOrder() throws Exception {
		var reversed = new Helper.DependsResult();
		Helper.dependsGTable(HostAlpha.class, gtableVar("managers"), "string", "string", "dynamic", reversed);
		Helper.dependsGTable(HostZulu.class, gtableVar("items"), "string", "string", "dynamic", reversed);

		assertEquals(whereOf(HostAlpha.class, "managers"),
				reversed.map2MetasDynamic.values().iterator().next().where,
				"反序到达的外层终胜者必须与正序相同（argmin(where) 与到达序无关）");
	}
}
