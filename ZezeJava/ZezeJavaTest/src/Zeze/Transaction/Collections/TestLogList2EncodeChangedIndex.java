package Zeze.Transaction.Collections;

import java.util.List;

import demo.Module1.BValue;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.TreePVector;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.OutInt;

/**
 * LogList2.encode 的 changed 过滤：对每个 changed bean 按身份在最终列表中定位
 * 下标，不在列表（或本轮add）的剔除。定位语义回归钉：身份定位、重复元素取
 * 首个出现位置（对齐原线性扫的break语义）、死bean剔除——下标预建优化
 * （IdentityHashMap一遍）不得改变这些语义。
 */
@Fast
public class TestLogList2EncodeChangedIndex {

	private static LogList2<BValue> newLog(List<BValue> finalList) {
		// belong/self/meta仅为encode之外的场景服务，encode只读ctor传入的最终列表
		return new LogList2<>(null, 0, null, TreePVector.from(finalList), null);
	}

	@Test
	public void testChangedIndexByIdentity() {
		var a = new BValue();
		var b = new BValue();
		var c = new BValue();
		var log = newLog(List.of(a, b, c));
		var vlogB = new LogBean(null, 0, b);
		log.getChanged().put(vlogB, new OutInt(99));

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(1, log.getChanged().size());
		Assertions.assertEquals(1, log.getChanged().get(vlogB).value, "按身份定位到下标1");
	}

	@Test
	public void testDuplicateElementKeepsFirstIndex() {
		var a = new BValue();
		var other = new BValue();
		var log = newLog(List.of(a, other, a)); // 同一引用出现两次
		var vlogA = new LogBean(null, 0, a);
		log.getChanged().put(vlogA, new OutInt(99));

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(0, log.getChanged().get(vlogA).value,
				"重复元素取首个出现位置（对齐原线性扫break语义）");
	}

	@Test
	public void testDeadBeanEntryRemoved() {
		var a = new BValue();
		var dead = new BValue(); // 不在最终列表
		var log = newLog(List.of(a));
		var vlogDead = new LogBean(null, 0, dead);
		log.getChanged().put(vlogDead, new OutInt(99));

		log.encode(ByteBuffer.Allocate());

		Assertions.assertEquals(0, log.getChanged().size(), "身份不在最终列表的changed条目必须剔除");
	}

	@Test
	public void testEmptyChangedEncodesZeroCount() {
		var log = newLog(List.of(new BValue()));
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		Assertions.assertEquals(0, bb.ReadUInt(), "空changed编码为计数0");
		Assertions.assertEquals(0, bb.ReadUInt(), "空opLogs编码为计数0");
	}
}
