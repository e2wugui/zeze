package Zeze.History;

import harness.Extra;
import demo.Module1.BItem;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 验证 History 能注册仅通过动态字段可达的 Bean。 */
@Fast
@Extra
public class TestHelperDynamicBeanTraversed {


	@Test
	public void testDynamicOnlyBeanRegistered() throws Exception {
		var result = new Helper.DependsResult();
		Helper.dependsBean(BValue.class, result);
		Assertions.assertTrue(result.beans.contains(BItem.class),
				"仅经 dynamic14 可达的 BItem 必须注册（缺失则其集合日志 typeId 永不注册，"
						+ "回放解码抛 unknown log typeId 卡死游标）");
		Assertions.assertTrue(result.allBeans.contains(BItem.class), "全量集合同样必须包含");
	}
}
