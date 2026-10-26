package Zeze.Transaction.Collections;

import java.util.List;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Savepoint;

/**
 * LogList2.addChild 的 changed 过滤：对每个 changed bean 按身份在最终列表中定位
 * 下标，不在列表（或本轮add）的剔除。定位语义回归钉：身份定位、重复元素取
 * 首个出现位置（对齐原线性扫的break语义）、死bean剔除——下标预建优化
 * （IdentityHashMap一遍）不得改变这些语义。
 */
@Fast
public class TestLogList2EncodeChangedIndex {

	@SuppressWarnings("DataFlowIssue") // 此encode夹具只使用value/changed，不访问meta。
	private static LogList2<BValue> newLog(List<BValue> finalList) {
		// belong/self/meta仅为encode之外的场景服务，encode只读ctor传入的最终列表
		var list = new PList2<BValue>(BValue.class);
		list.addAll(finalList);
		return (LogList2<BValue>)list.createLogBean(null);
	}

	@Test
	public void testChangedIndexByIdentity() {
		var a = new BValue();
		var b = new BValue();
		var c = new BValue();
		var log = newLog(List.of(a, b, c));
		var vlogB = new LogBean(0, b);
		log.addChild(log.getThis(), vlogB);

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(1, log.getChanged().size());
		Assertions.assertEquals(1, log.getChanged().get(vlogB).value, "按身份定位到下标1");
	}

	@Test
	public void testRepeatedChildRetainsAllFieldChanges() {
		Zeze.History.Helper.registerLogs();
		var value = new BValue();
		var log = newLog(List.of(value));
		var source = log.getThis();
		Assertions.assertNotNull(source);
		var child = new LogBean(0, value);
		child.getVariablesOrNew().put(1, Map2TestSupport.intLog(1, 7));
		Assertions.assertTrue(log.addChild(source, child));
		child.getVariablesOrNew().put(2, Map2TestSupport.longLog(2, 11));
		Assertions.assertFalse(log.addChild(source, child));
		var encoded = ByteBuffer.Allocate();
		log.encode(encoded);
		var decoded = LogList2.decode(log.getVariableId(), log.meta, encoded);
		var follower = new PList2<BValue>(BValue.class);
		follower.add(new BValue());
		follower.followerApply(decoded);
		Assertions.assertTrue(encoded.isEmpty());
		Assertions.assertEquals(1, decoded.getChanged().size());
		Assertions.assertEquals(7, follower.get(0).getInt_1());
		Assertions.assertEquals(11, follower.get(0).getLong2());
	}

	@Test
	public void testDuplicateElementKeepsFirstIndex() {
		var a = new BValue();
		var other = new BValue();
		var log = newLog(List.of(a, other, a)); // 同一引用出现两次
		var vlogA = new LogBean(0, a);
		log.addChild(log.getThis(), vlogA);

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(0, log.getChanged().get(vlogA).value,
			"重复元素取首个出现位置（对齐原线性扫break语义）");
	}

	@Test
	public void testDeadBeanEntryRemoved() {
		var a = new BValue();
		var dead = new BValue(); // 不在最终列表
		var log = newLog(List.of(a));
		var vlogDead = new LogBean(0, dead);
		log.addChild(log.getThis(), vlogDead);

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(0, log.getChanged().size(), "身份不在最终列表的changed条目必须剔除");
	}

	@Test
	public void testNullAndDifferentBeanTypeAreRemoved() {
		var log = newLog(List.of(new BValue()));
		log.addChild(log.getThis(), new LogBean(0, null));
		log.addChild(log.getThis(), new LogBean(0, new EmptyBean()));

		log.encode(ByteBuffer.Allocate());

		Assertions.assertTrue(log.getChanged().isEmpty());
	}

	@Test
	public void testAddedBeanChangeIsCarriedByStructuralOperation() {
		var list = new PList2<BValue>(BValue.class);
		var added = new BValue();
		var delta = new LogList2Delta<BValue>(list, list.getList());
		delta.add(added);
		delta.commit();
		var log = (LogList2<BValue>)list.createLogBean(delta);
		log.addChild(log.getThis(), new LogBean(0, added));

		var bb = ByteBuffer.Allocate();
		log.encode(bb);

		Assertions.assertTrue(log.getChanged().isEmpty());
		Assertions.assertEquals(0, bb.ReadUInt());
		Assertions.assertEquals(1, bb.ReadUInt());
	}

	@Test
	public void testEmptyChangedEncodesZeroCount() {
		var log = newLog(List.of(new BValue()));
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		Assertions.assertEquals(0, bb.ReadUInt(), "空changed编码为计数0");
		Assertions.assertEquals(0, bb.ReadUInt(), "空opLogs编码为计数0");
	}

	@Test
	public void testNestedClearDropsParentAddedIdentities() {
		var list = new PList2<BValue>(BValue.class);
		Map2TestSupport.attach(list);
		var delta = new LogList2Delta<BValue>(list, list.getList());
		delta.add(new BValue());
		var savepoint = new Savepoint();
		savepoint.putLog(delta);
		var nested = delta.beginSavepoint();
		nested.clear();
		nested.endSavepoint(savepoint);

		var added = delta.addedIdentities();
		Assertions.assertTrue(added == null || added.isEmpty(),
			"子保存点CLEAR后不得保留父保存点的新增身份");
		Assertions.assertEquals(1, delta.getOpLogs().size());
		Assertions.assertEquals(LogList1.OpLog.OP_CLEAR, delta.getOpLogs().getFirst().op);
	}

	@Test
	public void testNestedClearKeepsOnlySubsequentAddedIdentities() {
		var list = new PList2<BValue>(BValue.class);
		Map2TestSupport.attach(list);
		var delta = new LogList2Delta<BValue>(list, list.getList());
		var stale = new BValue();
		delta.add(stale);
		var savepoint = new Savepoint();
		savepoint.putLog(delta);
		var nested = delta.beginSavepoint();
		nested.clear();
		var surviving = new BValue();
		nested.add(surviving);
		nested.endSavepoint(savepoint);

		var added = delta.addedIdentities();
		Assertions.assertNotNull(added);
		Assertions.assertFalse(added.contains(stale), "CLEAR前新增身份不得残留");
		Assertions.assertTrue(added.contains(surviving), "CLEAR后新增身份必须保留");
		Assertions.assertEquals(1, added.size());
		Assertions.assertEquals(2, delta.getOpLogs().size());
		Assertions.assertEquals(LogList1.OpLog.OP_CLEAR, delta.getOpLogs().get(0).op);
		Assertions.assertEquals(LogList1.OpLog.OP_ADD, delta.getOpLogs().get(1).op);
		Assertions.assertSame(surviving, delta.getOpLogs().get(1).value);
	}
}
