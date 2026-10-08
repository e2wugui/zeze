package Zeze.History;

import harness.Extra;
import demo.Module1.BItem;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND24 hist-01 回归：dependsBean 对 "dynamic" 类型变量不得跳过——生成器把 dynamic
 * 变量的允许 bean 集写入 BVariable.Data 的 value 字段（逗号分隔），遍历按它递归注册；
 * 元数据缺失或跳过时，仅经 dynamic 可达的 bean（demo.Module1.BItem，未被任何静态
 * 变量引用）的集合/BeanKey 日志工厂永不注册，回放端 Log.create 抛 unknown log
 * typeId，毒记录确定性卡死游标。
 */
@Fast
@Extra
public class TestHelperDynamicBeanTraversed {

	/** 仅经 dynamic 可达的 bean 必须被 dependsBean 注册（修复前 dynamic 被 isBuiltinType
	 * 吞掉直接跳过，且元数据 value 为空串无数据源可遍历）。 */
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
