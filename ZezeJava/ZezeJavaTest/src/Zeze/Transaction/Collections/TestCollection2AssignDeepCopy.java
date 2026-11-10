package Zeze.Transaction.Collections;

import static java.util.Objects.requireNonNull;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.DynamicField;
import Zeze.Transaction.GTable.GTable2;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * assign 只存在于1系容器（PList1/PMap1/PSet1/PSortedMap1，引用共享语义），
 * 2系（PList2/PMap2/PSortedMap2）缺失同名API——用户从1系迁到2系时无对应写法。
 * 2系值为可变Bean，assign 语义为深拷贝元素赋值（复用 copy()）：
 * assign 后目标与源互不影响。
 */
@Fast
public class TestCollection2AssignDeepCopy {

	@Test
	public void testPMap2AssignDeepCopy() {
		var src = new PMap2<>(Long.class, BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.put(1L, srcBean);

		var dst = new PMap2<>(Long.class, BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, requireNonNull(dst.get(1L)).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(1L), "深拷贝：不得共享元素实例");

		srcBean.setInt_1(99); // 修改源不影响目标
		Assertions.assertEquals(11, requireNonNull(dst.get(1L)).getInt_1());
		requireNonNull(dst.get(1L)).setInt_1(22); // 修改目标不影响源
		Assertions.assertEquals(99, requireNonNull(src.get(1L)).getInt_1());
	}

	@Test
	public void testPMap2AssignReplacesExisting() {
		var src = new PMap2<>(Long.class, BValue.class);
		var b = new BValue();
		b.setInt_1(1);
		src.put(1L, b);
		var dst = new PMap2<>(Long.class, BValue.class);
		dst.put(2L, new BValue()); // 目标原有条目须被清空

		dst.assign(src);

		Assertions.assertEquals(1, dst.size());
		Assertions.assertTrue(dst.containsKey(1L));
	}

	@Test
	public void testPList2AssignDeepCopy() {
		var src = new PList2<>(BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.add(srcBean);

		var dst = new PList2<>(BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, dst.get(0).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(0));
		srcBean.setInt_1(99);
		Assertions.assertEquals(11, dst.get(0).getInt_1());
		dst.get(0).setInt_1(22);
		Assertions.assertEquals(99, src.get(0).getInt_1());
	}

	@Test
	public void testPSortedMap2AssignDeepCopy() {
		var src = new PSortedMap2<>(Long.class, BValue.class);
		var srcBean = new BValue();
		srcBean.setInt_1(11);
		src.put(1L, srcBean);

		var dst = new PSortedMap2<>(Long.class, BValue.class);
		dst.assign(src);

		Assertions.assertEquals(11, requireNonNull(dst.get(1L)).getInt_1());
		Assertions.assertNotSame(srcBean, dst.get(1L));
		srcBean.setInt_1(99);
		Assertions.assertEquals(11, requireNonNull(dst.get(1L)).getInt_1());
		requireNonNull(dst.get(1L)).setInt_1(22);
		Assertions.assertEquals(99, requireNonNull(src.get(1L)).getInt_1());
	}

	@Test
	public void remapsSpecialIdAcrossCollectionFields() {
		var sourceField = AssignmentFields.SOURCE;
		var targetField = AssignmentFields.TARGET;

		var sourceList = new PList2<>(AssignmentFields.SOURCE_LIST);
		var targetList = new PList2<>(AssignmentFields.TARGET_LIST);
		var sourceValue = dynamicValue(sourceField);
		sourceList.add(sourceValue);
		targetList.assign(sourceList);
		assertRemapped(sourceValue, targetList.get(0));

		var sourceMap = new PMap2<>(Map2Meta.createDynamic(Long.class, sourceField));
		var targetMap = new PMap2<>(Map2Meta.createDynamic(Long.class, targetField));
		sourceMap.put(1L, sourceValue.copy());
		targetMap.assign(sourceMap);
		assertRemapped(requireNonNull(sourceMap.get(1L)), requireNonNull(targetMap.get(1L)));

		var sourceSorted = new PSortedMap2<>(SortedMap2Meta.createDynamic(Long.class, sourceField));
		var targetSorted = new PSortedMap2<>(SortedMap2Meta.createDynamic(Long.class, targetField));
		sourceSorted.put(1L, sourceValue.copy());
		targetSorted.assign(sourceSorted);
		assertRemapped(requireNonNull(sourceSorted.get(1L)), requireNonNull(targetSorted.get(1L)));

		var sourceGrid = new GTable2<>(GTable2.getFactory(Integer.class, Integer.class, sourceField));
		var targetGrid = new GTable2<>(GTable2.getFactory(Integer.class, Integer.class, targetField));
		sourceGrid.put(1, 1, sourceValue.copy());
		targetGrid.assign(sourceGrid);
		assertRemapped(requireNonNull(sourceGrid.get(1, 1)), requireNonNull(targetGrid.get(1, 1)));
	}

	@Test
	public void sameFieldAssignPreservesDecodedSpecialIdAndCrossFieldAssignKeepsEmptyValues() {
		var source = new PList2<>(AssignmentFields.SOURCE_LIST);
		var alias = source.createValue();
		var bytes = ByteBuffer.Allocate();
		bytes.WriteLong(10);
		new BValue().encode(bytes);
		alias.decode(bytes);
		source.add(alias);
		source.add(source.createValue());

		var sameField = new PList2<>(AssignmentFields.SOURCE_LIST);
		sameField.assign(source);
		Assertions.assertEquals(10, sameField.get(0).getTypeId());

		var target = new PList2<>(AssignmentFields.TARGET_LIST);
		target.assign(source);
		Assertions.assertEquals(7, target.get(0).getTypeId());
		Assertions.assertTrue(target.get(1).isEmpty());
	}

	@Test
	public void rejectsUnsupportedBeanBeforeReplacingTarget() {
		var source = new PList2<>(AssignmentFields.SOURCE_LIST);
		source.add(dynamicValue(AssignmentFields.SOURCE));
		var unsupported = source.createValue();
		unsupported.setBean(new BSimple());
		source.add(unsupported);

		var target = new PList2<>(AssignmentFields.TARGET_LIST);
		var original = dynamicValue(AssignmentFields.TARGET);
		target.add(original);
		Assertions.assertThrows(IllegalArgumentException.class, () -> target.assign(source));
		Assertions.assertEquals(1, target.size());
		Assertions.assertSame(original, target.get(0));
	}

	private abstract static class AssignmentFields extends Bean {
		private static final long TYPE_ID = Bean.hash64(AssignmentFields.class.getName());
		private static final DynamicField SOURCE = DynamicField.declare(AssignmentFields.class, TYPE_ID, 1,
				bean -> bean instanceof BValue ? 8 : 9,
				id -> {
					if (id == 8 || id == 10)
						return new BValue();
					return id == 9 ? new BSimple() : null;
				});
		private static final DynamicField TARGET = DynamicField.declare(AssignmentFields.class, TYPE_ID, 2,
				bean -> {
					if (bean instanceof BValue)
						return 7;
					throw new IllegalArgumentException("target only accepts BValue");
				}, id -> id == 7 ? new BValue() : null);
		private static final List2Meta<DynamicBean> SOURCE_LIST = List2Meta.createDynamic(SOURCE);
		private static final List2Meta<DynamicBean> TARGET_LIST = List2Meta.createDynamic(TARGET);
	}

	private static DynamicBean dynamicValue(DynamicField field) {
		var value = new DynamicBean(field);
		var bean = new BValue();
		bean.setInt_1(11);
		value.setBean(bean);
		return value;
	}

	private static void assertRemapped(DynamicBean source, DynamicBean target) {
		Assertions.assertEquals(8, source.getTypeId());
		Assertions.assertEquals(7, target.getTypeId());
		Assertions.assertNotSame(source.getBean(), target.getBean());
		Assertions.assertEquals(11, ((BValue)target.getBean()).getInt_1());
	}

	/**
	 * 自赋值不丢内容：旧实现clear()后再copy(this)时源已空，原有元素永久丢失。
	 * 先备输入再发布（copy成功后才clear+装回）同时保证copy抛出时目标保持原值。
	 */
	@Test
	public void testSelfAssignKeepsContent() {
		var list = new PList2<>(BValue.class);
		var lb = new BValue();
		lb.setInt_1(1);
		list.add(lb);
		list.assign(list);
		Assertions.assertEquals(1, list.size(), "PList2自赋值不得清空（修复前：clear后copy已空的self）");
		Assertions.assertEquals(1, list.get(0).getInt_1());

		var map = new PMap2<>(Long.class, BValue.class);
		var mb = new BValue();
		mb.setInt_1(2);
		map.put(1L, mb);
		map.assign(map);
		Assertions.assertEquals(1, map.size(), "PMap2自赋值不得清空");
		Assertions.assertEquals(2, requireNonNull(map.get(1L)).getInt_1());

		var sorted = new PSortedMap2<>(Long.class, BValue.class);
		var sb = new BValue();
		sb.setInt_1(3);
		sorted.put(1L, sb);
		sorted.assign(sorted);
		Assertions.assertEquals(1, sorted.size(), "PSortedMap2自赋值不得清空");
		Assertions.assertEquals(3, requireNonNull(sorted.get(1L)).getInt_1());
	}
}
