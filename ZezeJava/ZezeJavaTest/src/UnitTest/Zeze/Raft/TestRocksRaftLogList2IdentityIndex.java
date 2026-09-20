package UnitTest.Zeze.Raft;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import Zeze.Raft.RocksRaft.Bean;
import Zeze.Raft.RocksRaft.Log;
import Zeze.Raft.RocksRaft.LogBean;
import Zeze.Raft.RocksRaft.LogList;
import Zeze.Raft.RocksRaft.LogList2;
import Zeze.Raft.RocksRaft.Record;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.OutInt;
import harness.Fast;

/**
 * RR2-F1 回归：RocksRaft LogList2.encode 以equals语义（indexOf）定位changed bean的
 * 最终下标，与经典 Transaction.Collections.LogList2 的身份扫描（v==bean）不一致。
 * 覆写equals的V（经典迁移bean必然如此）下两条受害路径：
 * ① 与已删bean equals相等的存活bean吃错下标（follower错位应用）；
 * ② 已删bean因equals命中存活条目而被误保留（死bean日志叠加应用）。
 * 修复：换成与经典一致的身份扫描；对身份equals的bean行为完全不变。
 * 直接构造LogList2并编码（setValue为包私有，反射设置，与TestRaftTermMaxReject同风格）。
 */
@Fast
public class TestRocksRaftLogList2IdentityIndex {

	/** 覆写equals的测试bean：按i值相等。 */
	public static final class BLeafEquals extends Bean {
		public int i;

		public BLeafEquals() { // SerializeHelper.createCodec需要无参构造
		}

		public BLeafEquals(int i) {
			this.i = i;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof BLeafEquals other && i == other.i;
		}

		@Override
		public int hashCode() {
			return i;
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(i);
		}

		@Override
		public void decode(IByteBuffer bb) {
			i = bb.ReadInt();
		}

		@Override
		public Bean copy() {
			return new BLeafEquals(i);
		}

		@Override
		public void followerApply(Log log) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void leaderApplyNoRecursive(Log log) {
		}
	}

	@SuppressWarnings("unchecked")
	private static LogList2<BLeafEquals> newLog(PVector<BLeafEquals> finalList) throws Exception {
		var log = new LogList2<>(BLeafEquals.class);
		// LogList.setValue包私有：反射设置最终列表（encode用它计算changed下标）
		Method setValue = LogList.class.getDeclaredMethod("setValue", PVector.class);
		setValue.setAccessible(true);
		setValue.invoke(log, finalList);
		return log;
	}

	private static LogList2<BLeafEquals> encodeDecodeRoundTrip(LogList2<BLeafEquals> log) {
		var bb = ByteBuffer.Allocate();
		log.encode(bb);
		var decoded = new LogList2<BLeafEquals>(BLeafEquals.class);
		decoded.decode(bb);
		return decoded;
	}

	/**
	 * 场景①：changed指向第2个bean（与第1个equals相等、身份不同），编码下标必须是
	 * 身份定位的1；equals语义的indexOf会错位到0，follower把第2个bean的日志错应用到
	 * 第1个bean上。
	 */
	@Test
	public void testEqualsSiblingGetsIdentityIndex() throws Exception {
		var leafA = new BLeafEquals(1);
		var leafB = new BLeafEquals(1); // equals(leafA)==true，身份不同

		var vlogB = new LogBean();
		vlogB.setThis(leafB);
		var log = newLog(TreePVector.from(List.of(leafA, leafB)));
		log.getChanged().put(vlogB, new OutInt());

		var decoded = encodeDecodeRoundTrip(log);
		Assertions.assertEquals(1, decoded.getChanged().size(), "存活bean的changed条目必须保留");
		var index = decoded.getChanged().values().iterator().next().value;
		Assertions.assertEquals(1, index, "changed下标必须按身份定位到1（equals语义会错位到0）");
	}

	/**
	 * 场景②：changed指向已被移除的死bean（与存活的第1个bean equals相等），encode必须
	 * 剔除该条目（身份不在最终列表）；equals语义的indexOf命中存活bean→误保留，
	 * follower把死bean的日志叠加应用到存活bean上。
	 */
	@Test
	public void testDeadEqualsBeanEntryRemoved() throws Exception {
		var leafA = new BLeafEquals(1);
		var leafDead = new BLeafEquals(1); // equals(leafA)==true，但已不在最终列表

		var vlogDead = new LogBean();
		vlogDead.setThis(leafDead);
		var log = newLog(TreePVector.from(List.of(leafA)));
		log.getChanged().put(vlogDead, new OutInt());

		var decoded = encodeDecodeRoundTrip(log);
		Assertions.assertEquals(0, decoded.getChanged().size(),
				"已移除bean（身份不在最终列表）的changed条目必须剔除，不得因equals命中存活bean而误保留");
	}

	/**
	 * 身份equals的bean（未覆写语义冲突场景）：正常存活条目的定位行为不变（锁定既有语义）。
	 */
	@Test
	public void testRegularEntryIndexUnchanged() throws Exception {
		var leafA = new BLeafEquals(1);
		var leafB = new BLeafEquals(2);

		var vlogB = new LogBean();
		vlogB.setThis(leafB);
		var log = newLog(TreePVector.from(List.of(leafA, leafB)));
		log.getChanged().put(vlogB, new OutInt());

		var decoded = encodeDecodeRoundTrip(log);
		Assertions.assertEquals(1, decoded.getChanged().size());
		Assertions.assertEquals(1, decoded.getChanged().values().iterator().next().value);
	}
}
