package Infinite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Net.Binary;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSet1;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.StableRandom;
import Zeze.Util.Task;
import demo.Bean1;
import demo.Module1.BFood;
import demo.Module1.BItem;
import demo.Module1.BRemoved2;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Key;
import org.jetbrains.annotations.Nullable;

public class CoverHistory {
	private static final int eKeyRange = 1024; // 修改的记录范围
	private static final int eTaskCont = 10000; // 总的修改任务
	private static final int eJobsPerTask = 3; // 每个任务执行多少个修改工作。
	private final demo.App app;
	private final ArrayList<Job> jobs = new ArrayList<>();
	private final ArrayList<Future<?>> taskFutures = new ArrayList<>();

	public CoverHistory(demo.App app) {
		this.app = app;
		jobs.add(new Int_1());
		jobs.add(new Long2());
		jobs.add(new String3());
		jobs.add(new Bool4());
		jobs.add(new Short5());
		jobs.add(new Float6());
		jobs.add(new Double7());
		jobs.add(new Bytes8());
		jobs.add(new List9());
		jobs.add(new Set10());
		jobs.add(new Map11());
		jobs.add(new Bean12());
		jobs.add(new Byte13());
		jobs.add(new Dynamic14());
		jobs.add(new Map15());
		jobs.add(new Map16());
		jobs.add(new Vector2());
		jobs.add(new Vector3Int());
		jobs.add(new Vector3());
		jobs.add(new Vector3Int());
		jobs.add(new Vector4());
		jobs.add(new Quaternion());
		jobs.add(new Dynamic23());
		jobs.add(new ListVector2Int());
		jobs.add(new Map25());
		jobs.add(new Map26());
		jobs.add(new Dynamic27());
		jobs.add(new Key28());
		jobs.add(new Array29());
		jobs.add(new List30());
		jobs.add(new List31());
		jobs.add(new List32());
		jobs.add(new List33());
		jobs.add(new List34());
		jobs.add(new List35());
		jobs.add(new List36());
		jobs.add(new List37());
		jobs.add(new Set38());
		jobs.add(new Set39());
		jobs.add(new Map40());
		jobs.add(new Map41());
		jobs.add(new Map42Recursive());
		jobs.add(new RemoveRecord());
		jobs.add(new Combo());
	}

	public void submitTasks() {
		var t = System.currentTimeMillis();
		for (var i = 0; i < eTaskCont; ++i) {
			var seed = t ^ i;
			taskFutures.add(Task.runUnsafe(app.Zeze.newProcedure(() -> runJobs(seed, null), "runJob")));
		}
	}

	public void join() throws ExecutionException, InterruptedException {
		for (var future : taskFutures)
			future.get();
		taskFutures.clear();
	}

	public static StableRandom getRandom() {
		return StableRandom.local();
	}

	/**
	 * 当访问记录的时候，r为null，里面随机获取记录。
	 * 当嵌套执行测试时，由外层Job提供嵌套的数据，典型的时Map<Key,BValue>可以对里面的值嵌套执行runJobs.
	 *
	 * @param r data accessed.
	 * @return long
	 */
	private long runJobs(long seed, @Nullable BValue r) {
		if (r == null)
			StableRandom.local().setSeed(seed);
		var jobIndexes = new HashSet<Integer>();
		var limit = eJobsPerTask;
		while (limit > 0) {
			var index = getRandom().nextInt(jobs.size());
			if (jobIndexes.add(index)) { // 随机可能重复，确保运行足够的limit次数。当jobs足够多，limit足够小，冲突应该不大。
				limit--;
				jobs.get(index).run(r != null ? r
						: app.demo_Module1.getTableCoverHistory().getOrAdd(getRandom().nextLong(eKeyRange)));
			}
		}
		return 0;
	}

	public interface Job {
		void run(BValue value);
	}

	public static class Int_1 implements Job {
		@Override
		public void run(BValue value) {
			value.setInt_1(getRandom().nextInt());
		}
	}

	public static class Long2 implements Job {
		@Override
		public void run(BValue value) {
			value.setLong2(getRandom().nextLong());
		}
	}

	public static class String3 implements Job {
		@Override
		public void run(BValue value) {
			value.setString3(String.valueOf(getRandom().nextInt()));
		}
	}

	public static class Bool4 implements Job {
		@Override
		public void run(BValue value) {
			value.setBool4(getRandom().nextInt(2) == 1);
		}
	}

	public static class Short5 implements Job {
		@Override
		public void run(BValue value) {
			value.setShort5((short)getRandom().nextInt(Short.MAX_VALUE));
		}
	}

	public static class Float6 implements Job {
		@Override
		public void run(BValue value) {
			value.setFloat6(getRandom().nextInt());
		}
	}

	public static class Double7 implements Job {
		@Override
		public void run(BValue value) {
			value.setDouble7(getRandom().nextInt(Short.MAX_VALUE));
		}
	}

	public static class Bytes8 implements Job {
		@Override
		public void run(BValue value) {
			var bytes = new byte[8];
			getRandom().nextBytes(bytes);
			value.setBytes8(new Binary(bytes));
		}
	}

	public static class List9 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList9();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(3)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Bean1());
					else
						list.add(getRandom().nextInt(list.size()), new Bean1());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;

				case 2: // modify.
					if (list.isEmpty())
						list.add(new Bean1()); // ensure modify
					var randItem = list.get(getRandom().nextInt(list.size()));
					randItem.setV1(getRandom().nextInt());
					break;
				}
			}
		}
	}

	public static class Set10 implements Job {

		private static void randRemove(PSet1<Integer> set) {
			if (set.isEmpty())
				return;

			var index = getRandom().nextInt(set.size());
			for (var it = set.iterator(); it.hasNext(); /**/) {
				if (index == 0) {
					it.remove();
					break;
				}
				--index;
				it.next();
			}
		}

		@Override
		public void run(BValue value) {
			var set = value.getSet10();
			var forceRemove = set.size() >= 3;
			if (forceRemove) {
				randRemove(set);
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					set.add(getRandom().nextInt());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public static class Map11 implements Job {
		@Override
		public void run(BValue value) {
			var map11 = value.getMap11(); // key is 0,1,2
			var forceRemove = map11.size() >= 3;
			if (forceRemove) {
				map11.remove(getRandom().nextLong(3));
			} else {
				switch (getRandom().nextInt(3)) {
				case 0: // put, maybe replace.
					map11.put(getRandom().nextLong(3), new demo.Module2.BValue());
					break;

				case 1: // remove, maybe remove nothing
					map11.remove(getRandom().nextLong(3));
					break;

				case 2: // modify.
					if (map11.isEmpty())
						map11.put(getRandom().nextLong(3), new demo.Module2.BValue()); // ensure modify
					var randItem = getRandItem(map11);
					assert randItem != null;
					randItem.setS(getRandom().nextInt());
					break;
				}
			}
		}

		private static demo.Module2.BValue getRandItem(PMap2<Long, demo.Module2.BValue> map2) {
			if (map2.isEmpty())
				return null;
			var index = getRandom().nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public static class Bean12 implements Job {

		@Override
		public void run(BValue value) {
			switch (getRandom().nextInt(6)) {
			case 0:
				value.getBean12().setInt_1(getRandom().nextInt());
				break;
			case 1:
				value.getBean12().setLong2(getRandom().nextLong());
				break;
			case 2:
				value.getBean12().setString3(String.valueOf(getRandom().nextInt()));
				break;
			case 3:
				value.getBean12().setRemoved(new BRemoved2());
				break;
			case 4:
				value.getBean12().getRemoved().setInt_1(getRandom().nextInt());
				break;
			case 5:
				value.setBean12(new BSimple());
				break;
			}
		}
	}

	public static class Byte13 implements Job {

		@Override
		public void run(BValue value) {
			value.setByte13((byte)getRandom().nextInt(255));
		}
	}

	public static class Dynamic14 implements Job {

		public static void testDynamic(DynamicBean dVar) {
			switch (getRandom().nextInt(5)) {
			case 0:
				dVar.setBean(new demo.Bean1());
				break;
			case 1:
				dVar.setBean(new BSimple());
				break;
			case 2:
				dVar.setBean(new BItem());
				break;
			case 3:
				var dBean = dVar.getBean();
				if (dBean.typeId() == Bean1.TYPEID)
					((Bean1)dBean).setV1(getRandom().nextInt());
				else if (dBean.typeId() == BSimple.TYPEID)
					((BSimple)dBean).setInt_1(getRandom().nextInt());
				else if (dBean.typeId() == BItem.TYPEID) {
					var bItem = (BItem)dBean;
					switch (getRandom().nextInt(3)) {
					case 0:
						bItem.setSubclass(new demo.Bean1());
						break;
					case 1:
						bItem.setSubclass(new BSimple());
						break;
					case 2:
						bItem.setSubclass(new BFood());
						break;
					}
				}
				break;
			case 4:
				if (dVar.getBean().typeId() == BItem.TYPEID) {
					var subClass = ((BItem)dVar.getBean()).getSubclass().getBean();
					if (subClass.typeId() == Bean1.TYPEID)
						((Bean1)subClass).setV1(getRandom().nextInt());
					else if (subClass.typeId() == BSimple.TYPEID)
						((BSimple)subClass).setInt_1(getRandom().nextInt());
					else if (subClass.typeId() == BFood.TYPEID)
						// 不再深入了。
						((BFood)subClass).setSubclass(new demo.Bean1());
				}
				break;
			}
		}

		@Override
		public void run(BValue value) {
			testDynamic(value.getDynamic14());
		}
	}

	public static class Map15 implements Job {
		@Override
		public void run(BValue value) {
			var map15 = value.getMap15(); // key is 0,1,2
			var forceRemove = map15.size() >= 3;
			if (forceRemove) {
				map15.remove(getRandom().nextLong(3)); // maybe remove nothing.
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // put, maybe replace.
					map15.put(getRandom().nextLong(3), getRandom().nextLong());
					break;

				case 1: // remove, maybe remove nothing
					map15.remove(getRandom().nextLong(3));
					break;
				}
			}
		}
	}

	public static class Map16 implements Job {
		public static Key randKey() {
			return new Key((short)getRandom().nextInt(3), "");
		}

		public static void testKey2BSimple(PMap2<Key, BSimple> map) {
			var forceRemove = map.size() >= 3;
			if (forceRemove) {
				map.remove(randKey());
			} else {
				switch (getRandom().nextInt(3)) {
				case 0: // put, maybe replace.
					map.put(randKey(), new BSimple());
					break;

				case 1: // remove, maybe remove nothing
					map.remove(randKey());
					break;

				case 2: // modify.
					if (map.isEmpty())
						map.put(randKey(), new BSimple()); // ensure modify
					var randItem = getRandItem(map);
					assert randItem != null;
					randItem.setInt_1(getRandom().nextInt());
					break;
				}
			}
		}

		@Override
		public void run(BValue value) {
			var map = value.getMap16(); // see randKey
			testKey2BSimple(map);
		}

		public static BSimple getRandItem(PMap2<Key, BSimple> map2) {
			if (map2.isEmpty())
				return null;
			var index = getRandom().nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public static class Vector2 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector2(new Zeze.Serialize.Vector2(getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Vector2Int implements Job {
		@Override
		public void run(BValue value) {
			value.setVector2Int(new Zeze.Serialize.Vector2Int(getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Vector3 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector3(new Zeze.Serialize.Vector3(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Vector3Int implements Job {
		@Override
		public void run(BValue value) {
			value.setVector3Int(new Zeze.Serialize.Vector3Int(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Vector4 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector4(new Zeze.Serialize.Vector4(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Quaternion implements Job {
		@Override
		public void run(BValue value) {
			value.setQuaternion(new Zeze.Serialize.Quaternion(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
		}
	}

	public static class Dynamic23 extends Dynamic14 {
		@Override
		public void run(BValue value) {
			testDynamic(value.getDynamic23());
		}
	}

	public static class ListVector2Int implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getListVector2Int();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2Int(getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector2Int(getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class Map25 implements Job {
		@Override
		public void run(BValue value) {
			var map = value.getMap25(); // see randKey
			Map16.testKey2BSimple(map);
		}
	}

	public static class Map26 implements Job {
		@Override
		public void run(BValue value) {
			var map = value.getMap26();
			var forceRemove = map.size() >= 3;
			if (forceRemove) {
				map.remove(Map16.randKey());
			} else {
				switch (getRandom().nextInt(3)) {
				case 0: // put, maybe replace.
					map.put(Map16.randKey(), BValue.newDynamicBean_Map26());
					break;

				case 1: // remove, maybe remove nothing
					map.remove(Map16.randKey());
					break;

				case 2: // modify.
					if (map.isEmpty())
						map.put(Map16.randKey(), BValue.newDynamicBean_Map26()); // ensure modify
					var randItem = getRandItem(map);
					assert randItem != null;
					var bean = randItem.getBean();
					if (bean.typeId() != BSimple.TYPEID)
						randItem.setBean(new BSimple());
					else
						((BSimple)bean).setInt_1(getRandom().nextInt());
					break;
				}
			}
		}

		private static DynamicBean getRandItem(PMap2<Key, DynamicBean> map2) {
			if (map2.isEmpty())
				return null;
			var index = getRandom().nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public static class Dynamic27 implements Job {

		@Override
		public void run(BValue value) {
			// dynamic14 已经测试过了，这里简单表现一下。
			switch (getRandom().nextInt(2)) {
			case 0:
				value.getDynamic27().setBean(new BSimple());
				break;
			case 1:
				var bean = value.getDynamic27().getBean();
				if (bean.typeId() == BSimple.TYPEID)
					((BSimple)bean).setInt_1(getRandom().nextInt());
				break;
			}
		}
	}

	public static class Key28 implements Job {

		@Override
		public void run(BValue value) {
			value.setKey28(new Key((short)getRandom().nextInt(100), String.valueOf(getRandom().nextInt())));
		}
	}

	public static class Array29 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getArray29();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add((float)getRandom().nextInt());
					else
						list.add(getRandom().nextInt(list.size()), (float)getRandom().nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List30 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList30();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(getRandom().nextInt());
					else
						list.add(getRandom().nextInt(list.size()), getRandom().nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List31 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getList31();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(getRandom().nextLong());
					else
						list.add(getRandom().nextInt(list.size()), getRandom().nextLong());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List32 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getList32();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add((float)getRandom().nextInt());
					else
						list.add(getRandom().nextInt(list.size()), (float)getRandom().nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List33 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList33();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2(getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector2(getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List34 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList34();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector3(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector3(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List35 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList35();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector4(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector4(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List36 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList36();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2Int(getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector2Int(getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class List37 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList37();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(getRandom().nextInt(list.size()));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector3Int(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					else
						list.add(getRandom().nextInt(list.size()), new Zeze.Serialize.Vector3Int(getRandom().nextInt(), getRandom().nextInt(), getRandom().nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(getRandom().nextInt(list.size()));
					break;
				}
			}
		}
	}

	public static class Set38 implements Job {
		private static void randRemove(PSet1<Integer> set) {
			if (set.isEmpty())
				return;

			var index = getRandom().nextInt(set.size());
			for (var it = set.iterator(); it.hasNext(); /**/) {
				if (index == 0) {
					it.remove();
					break;
				}
				--index;
				it.next();
			}
		}

		@Override
		public void run(BValue value) {
			var set = value.getSet38();
			var forceRemove = set.size() >= 3;
			if (forceRemove) {
				randRemove(set);
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					set.add(getRandom().nextInt());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public static class Set39 implements Job {
		private static void randRemove(PSet1<Long> set) {
			if (set.isEmpty())
				return;

			var index = getRandom().nextInt(set.size());
			for (var it = set.iterator(); it.hasNext(); /**/) {
				if (index == 0) {
					it.remove();
					break;
				}
				--index;
				it.next();
			}
		}

		@Override
		public void run(BValue value) {
			var set = value.getSet39();
			var forceRemove = set.size() >= 3;
			if (forceRemove) {
				randRemove(set);
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // insert
					set.add(getRandom().nextLong());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public static class Map40 implements Job {

		@Override
		public void run(BValue value) {
			var map40 = value.getMap40(); // key is 0,1,2
			var forceRemove = map40.size() >= 3;
			if (forceRemove) {
				map40.remove(getRandom().nextInt(3));
			} else {
				switch (getRandom().nextInt(2)) {
				case 0: // put, maybe replace.
					map40.put(getRandom().nextInt(3), getRandom().nextInt());
					break;

				case 1: // remove, maybe remove nothing
					map40.remove(getRandom().nextInt(3));
					break;
				}
			}
		}
	}

	public static class Map41 implements Job {
		@Override
		public void run(BValue value) {
			var map41 = value.getMap41(); // key is 0,1,2
			var forceRemove = map41.size() >= 3;
			if (forceRemove) {
				map41.remove(getRandom().nextLong(3));
			} else {
				switch (getRandom().nextInt(3)) {
				case 0: // put, maybe replace.
					map41.put(getRandom().nextLong(3), new BSimple());
					break;

				case 1: // remove, maybe remove nothing
					map41.remove(getRandom().nextLong(3));
					break;

				case 2: // modify.
					if (map41.isEmpty())
						map41.put(getRandom().nextLong(3), new BSimple()); // ensure modify
					var randItem = getRandItem(map41);
					assert randItem != null;
					randItem.setInt_1(getRandom().nextInt());
					break;
				}
			}
		}

		private static BSimple getRandItem(PMap2<Long, BSimple> map2) {
			if (map2.isEmpty())
				return null;
			var index = getRandom().nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public class Map42Recursive implements Job {
		@Override
		public void run(BValue value) {
			// map 基本测试足够了，这里只测试递归修改。
			var map = value.getMap42Recursive();
			var rValue = map.computeIfAbsent(0L, (key) -> new BValue());
			runJobs(0, rValue); // recursive, 没有退出限制，靠概率退出。
		}
	}

	// var 99 LongList 不测试了，跟List31一样。

	public class RemoveRecord implements Job {
		@Override
		public void run(BValue value) {
			app.demo_Module1.getTableCoverHistory().remove(getRandom().nextLong(eKeyRange));
		}
	}

	public class Combo implements Job {
		@Override
		public void run(BValue value) {
			var k = getRandom().nextLong(eKeyRange);
			app.demo_Module1.getTableCoverHistory().getOrAdd(k).setLong2(12345);
			app.demo_Module1.getTableCoverHistory().remove(k);
			app.demo_Module1.getTableCoverHistory().getOrAdd(k).setLong2(54321);
		}
	}
}
