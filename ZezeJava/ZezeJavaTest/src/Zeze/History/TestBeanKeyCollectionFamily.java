package Zeze.History;

import harness.Extra;
import Zeze.Builtin.AutoKey.BSeedKey;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 集合value为BeanKey时回放日志工厂的选族回归守卫：生成器规则IsNormalBean?'2':'1'
 * ——beankey非normal bean，值集合必须登记"1"族（list1/map1/sortedMap1/GTable1的内层
 * BeanMap1）；误入"2"族时运行时按"1"族工厂记录的typeId在回放端查不到，解码抛
 * unknown log typeId（回放/Verify确定性中断）。直驱Helper.depends*断言族归属。
 */
@Fast
@Extra
public class TestBeanKeyCollectionFamily {

	private static final String BEAN_KEY = BSeedKey.class.getName();

	private static BVariable.Data var() {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName("members");
		v.setKey("string");
		v.setValue(BEAN_KEY);
		return v;
	}

	@Test
	public void testListValueBeanKeyRegistersFamily1() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsList(TestBeanKeyCollectionFamily.class, var(), BEAN_KEY, result);
		Assertions.assertTrue(result.list1.contains(BSeedKey.class), "beankey值list须落1族（PList1/LogList1）");
		Assertions.assertTrue(result.list2.isEmpty(), "误入2族时beankey不是Bean，回放解码必炸");
	}

	@Test
	public void testMapValueBeanKeyRegistersFamily1() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsMap(TestBeanKeyCollectionFamily.class, var(), "string", BEAN_KEY, result);
		Assertions.assertTrue(result.map1.contains(KV.create(String.class, BSeedKey.class)),
				"beankey值map须落1族（PMap1/LogMap1）");
		Assertions.assertTrue(result.map2.isEmpty());
	}

	@Test
	public void testSortedMapValueBeanKeyRegistersFamily1() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsSortedMap(TestBeanKeyCollectionFamily.class, var(), "long", BEAN_KEY, result);
		Assertions.assertTrue(result.sortedMap1.contains(KV.create(Long.class, BSeedKey.class)),
				"beankey值sortedMap须落1族（PSortedMap1/LogSortedMap1）");
		Assertions.assertTrue(result.sortedMap2.isEmpty());
	}

	@Test
	public void testGTableValueBeanKeyRegistersFamily1() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsGTable(TestBeanKeyCollectionFamily.class, var(), "string", "long", BEAN_KEY, result);
		// GTable1的内层是BeanMap1（BmapMeta落map1Metas）；bean值时两个meta都落map2Metas。
		Assertions.assertEquals(1, result.map1Metas.size(), "beankey值gtable的内层BmapMeta须落map1Metas");
		Assertions.assertEquals(1, result.map2Metas.size(), "gtable外层PmapMeta恒落map2Metas");
	}
}
