package Infinite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Net.Binary;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSet1;
import Zeze.Transaction.DynamicBean;
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
	private final Random random = new Random();
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
	}

	public void submitTasks() {
		for (var i = 0; i < eTaskCont; ++i)
			taskFutures.add(Task.runUnsafe(app.Zeze.newProcedure(() -> runJobs(null), "runJob")));
	}

	public void join() throws ExecutionException, InterruptedException {
		for (var future : taskFutures)
			future.get();
		taskFutures.clear();
	}

	/**
	 * 当访问记录的时候，r为null，里面随机获取记录。
	 * 当嵌套执行测试时，由外层Job提供嵌套的数据，典型的时Map<Key,BValue>可以对里面的值嵌套执行runJobs.
	 * @param r data accessed.
	 * @return long
	 */
	private long runJobs(@Nullable BValue r) {
		var jobIndexes = new HashSet<Integer>();
		var limit = eJobsPerTask;
		while (limit > 0) {
			var index = random.nextInt(jobs.size());
			if (jobIndexes.add(index)) { // 随机可能重复，确保运行足够的limit次数。当jobs足够多，limit足够小，冲突应该不大。
				limit --;
				jobs.get(index).run(r != null ? r
						: app.demo_Module1.getTableCoverHistory().getOrAdd(random.nextLong(eKeyRange)));
			}
		}
		return 0;
	}

	public interface Job {
		void run(BValue value);
	}

	public class Int_1 implements Job {
		@Override
		public void run(BValue value) {
			value.setInt_1(random.nextInt());
		}
	}

	public class Long2 implements Job {
		@Override
		public void run(BValue value) {
			value.setLong2(random.nextLong());
		}
	}

	public class String3 implements Job {
		@Override
		public void run(BValue value) {
			value.setString3(String.valueOf(random.nextInt()));
		}
	}

	public class Bool4 implements Job {
		@Override
		public void run(BValue value) {
			value.setBool4(random.nextInt(2) == 1);
		}
	}

	public class Short5 implements Job {
		@Override
		public void run(BValue value) {
			value.setShort5((short)random.nextInt(Short.MAX_VALUE));
		}
	}

	public class Float6 implements Job {
		@Override
		public void run(BValue value) {
			value.setFloat6(random.nextInt());
		}
	}

	public class Double7 implements Job {
		@Override
		public void run(BValue value) {
			value.setDouble7(random.nextInt(Short.MAX_VALUE));
		}
	}

	public class Bytes8 implements Job {
		@Override
		public void run(BValue value) {
			var bytes = new byte[8];
			random.nextBytes(bytes);
			value.setBytes8(new Binary(bytes));
		}
	}

	public class List9 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList9();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(3)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Bean1());
					else
						list.add(random.nextInt(list.size()), new Bean1());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;

				case 2: // modify.
					if (list.isEmpty())
						list.add(new Bean1()); // ensure modify
					var randItem = list.get(random.nextInt(list.size()));
					randItem.setV1(random.nextInt());
					break;
				}
			}
		}
	}

	public class Set10 implements Job {

		private void randRemove(PSet1<Integer> set) {
			if (set.isEmpty())
				return;

			var index = random.nextInt(set.size());
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
				switch (random.nextInt(2)) {
				case 0: // insert
					set.add(random.nextInt());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public class Map11 implements Job {
		@Override
		public void run(BValue value) {
			var map11 = value.getMap11(); // key is 0,1,2
			var forceRemove = map11.size() >= 3;
			if (forceRemove) {
				map11.remove(random.nextLong(3));
			} else {
				switch (random.nextInt(3)) {
				case 0: // put, maybe replace.
					map11.put(random.nextLong(3), new demo.Module2.BValue());
					break;

				case 1: // remove, maybe remove nothing
					map11.remove(random.nextLong(3));
					break;

				case 2: // modify.
					if (map11.isEmpty())
						map11.put(random.nextLong(3), new demo.Module2.BValue()); // ensure modify
					var randItem = getRandItem(map11);
					assert randItem != null;
					randItem.setS(random.nextInt());
					break;
				}
			}
		}

		private demo.Module2.BValue getRandItem(PMap2<Long, demo.Module2.BValue> map2) {
			if (map2.isEmpty())
				return null;
			var index = random.nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public class Bean12 implements Job {

		@Override
		public void run(BValue value) {
			switch (random.nextInt(6)) {
			case 0:
				value.getBean12().setInt_1(random.nextInt());
				break;
			case 1:
				value.getBean12().setLong2(random.nextLong());
				break;
			case 2:
				value.getBean12().setString3(String.valueOf(random.nextInt()));
				break;
			case 3:
				value.getBean12().setRemoved(new BRemoved2());
				break;
			case 4:
				value.getBean12().getRemoved().setInt_1(random.nextInt());
				break;
			case 5:
				value.setBean12(new BSimple());
				break;
			}
		}
	}

	public class Byte13 implements Job {

		@Override
		public void run(BValue value) {
			value.setByte13((byte)random.nextInt(255));
		}
	}

	public class Dynamic14 implements Job {

		public static void testDynamic(Random random, DynamicBean dVar) {
			switch (random.nextInt(5)) {
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
					((Bean1)dBean).setV1(random.nextInt());
				else if (dBean.typeId() == BSimple.TYPEID)
					((BSimple)dBean).setInt_1(random.nextInt());
				else if (dBean.typeId() == BItem.TYPEID) {
					var bItem = (BItem)dBean;
					switch (random.nextInt(3)) {
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
						((Bean1)subClass).setV1(random.nextInt());
					else if (subClass.typeId() == BSimple.TYPEID)
						((BSimple)subClass).setInt_1(random.nextInt());
					else if (subClass.typeId() == BFood.TYPEID)
						// 不再深入了。
						((BFood)subClass).setSubclass(new demo.Bean1());
				}
				break;
			}
		}

		@Override
		public void run(BValue value) {
			testDynamic(random, value.getDynamic14());
		}
	}

	public class Map15 implements Job {
		@Override
		public void run(BValue value) {
			var map15 = value.getMap15(); // key is 0,1,2
			var forceRemove = map15.size() >= 3;
			if (forceRemove) {
				map15.remove(random.nextLong(3)); // maybe remove nothing.
			} else {
				switch (random.nextInt(2)) {
				case 0: // put, maybe replace.
					map15.put(random.nextLong(3), random.nextLong());
					break;

				case 1: // remove, maybe remove nothing
					map15.remove(random.nextLong(3));
					break;
				}
			}
		}
	}

	public class Map16 implements Job {
		public static Key randKey(Random random) {
			return new Key((short)random.nextInt(3), "");
		}

		public static void testKey2BSimple(Random random, PMap2<Key, BSimple> map) {
			var forceRemove = map.size() >= 3;
			if (forceRemove) {
				map.remove(randKey(random));
			} else {
				switch (random.nextInt(3)) {
				case 0: // put, maybe replace.
					map.put(randKey(random), new BSimple());
					break;

				case 1: // remove, maybe remove nothing
					map.remove(randKey(random));
					break;

				case 2: // modify.
					if (map.isEmpty())
						map.put(randKey(random), new BSimple()); // ensure modify
					var randItem = getRandItem(random, map);
					assert randItem != null;
					randItem.setInt_1(random.nextInt());
					break;
				}
			}
		}

		@Override
		public void run(BValue value) {
			var map = value.getMap16(); // see randKey
			testKey2BSimple(random, map);
		}

		public static BSimple getRandItem(Random random, PMap2<Key, BSimple> map2) {
			if (map2.isEmpty())
				return null;
			var index = random.nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public class Vector2 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector2(new Zeze.Serialize.Vector2(random.nextInt(), random.nextInt()));
		}
	}

	public class Vector2Int implements Job {
		@Override
		public void run(BValue value) {
			value.setVector2Int(new Zeze.Serialize.Vector2Int(random.nextInt(), random.nextInt()));
		}
	}

	public class Vector3 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector3(new Zeze.Serialize.Vector3(random.nextInt(), random.nextInt(), random.nextInt()));
		}
	}

	public class Vector3Int implements Job {
		@Override
		public void run(BValue value) {
			value.setVector3Int(new Zeze.Serialize.Vector3Int(random.nextInt(), random.nextInt(), random.nextInt()));
		}
	}

	public class Vector4 implements Job {
		@Override
		public void run(BValue value) {
			value.setVector4(new Zeze.Serialize.Vector4(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt()));
		}
	}

	public class Quaternion implements Job {
		@Override
		public void run(BValue value) {
			value.setQuaternion(new Zeze.Serialize.Quaternion(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt()));
		}
	}

	public class Dynamic23 extends Dynamic14 {
		@Override
		public void run(BValue value) {
			testDynamic(random, value.getDynamic23());
		}
	}

	public class ListVector2Int implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getListVector2Int();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2Int(random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector2Int(random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class Map25 implements Job {
		@Override
		public void run(BValue value) {
			var map = value.getMap25(); // see randKey
			Map16.testKey2BSimple(random, map);
		}
	}

	public class Map26 implements Job {
		@Override
		public void run(BValue value) {
			var map = value.getMap26();
			var forceRemove = map.size() >= 3;
			if (forceRemove) {
				map.remove(Map16.randKey(random));
			} else {
				switch (random.nextInt(3)) {
				case 0: // put, maybe replace.
					map.put(Map16.randKey(random), BValue.newDynamicBean_Map26());
					break;

				case 1: // remove, maybe remove nothing
					map.remove(Map16.randKey(random));
					break;

				case 2: // modify.
					if (map.isEmpty())
						map.put(Map16.randKey(random), BValue.newDynamicBean_Map26()); // ensure modify
					var randItem = getRandItem(map);
					assert randItem != null;
					var bean = randItem.getBean();
					if (bean.typeId() != BSimple.TYPEID)
						randItem.setBean(new BSimple());
					else
						((BSimple)bean).setInt_1(random.nextInt());
					break;
				}
			}
		}

		private DynamicBean getRandItem(PMap2<Key, DynamicBean> map2) {
			if (map2.isEmpty())
				return null;
			var index = random.nextInt(map2.size());
			for (var cur : map2) {
				if (index-- == 0)
					return cur.getValue();
			}
			return null;
		}
	}

	public class Dynamic27 implements Job {

		@Override
		public void run(BValue value) {
			// dynamic14 已经测试过了，这里简单表现一下。
			switch (random.nextInt(2)) {
			case 0:
				value.getDynamic27().setBean(new BSimple());
				break;
			case 1:
				var bean = value.getDynamic27().getBean();
				if (bean.typeId() == BSimple.TYPEID)
					((BSimple)bean).setInt_1(random.nextInt());
				break;
			}
		}
	}

	public class Key28 implements Job {

		@Override
		public void run(BValue value) {
			value.setKey28(new Key((short)random.nextInt(100), String.valueOf(random.nextInt())));
		}
	}

	public class Array29 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getArray29();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add((float)random.nextInt());
					else
						list.add(random.nextInt(list.size()), (float)random.nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List30 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList30();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(random.nextInt());
					else
						list.add(random.nextInt(list.size()), random.nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List31 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getList31();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(random.nextLong());
					else
						list.add(random.nextInt(list.size()), random.nextLong());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List32 implements Job {

		@Override
		public void run(BValue value) {
			var list = value.getList32();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add((float)random.nextInt());
					else
						list.add(random.nextInt(list.size()), (float)random.nextInt());
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List33 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList33();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2(random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector2(random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List34 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList34();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector3(random.nextInt(), random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector3(random.nextInt(), random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List35 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList35();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector4(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector4(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List36 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList36();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector2Int(random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector2Int(random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class List37 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList37();
			var forceRemove = list.size() >= 3;
			if (forceRemove) {
				list.remove(random.nextInt(list.size()));
			} else {
				switch (random.nextInt(2)) {
				case 0: // insert
					if (list.isEmpty())
						list.add(new Zeze.Serialize.Vector3Int(random.nextInt(), random.nextInt(), random.nextInt()));
					else
						list.add(random.nextInt(list.size()), new Zeze.Serialize.Vector3Int(random.nextInt(), random.nextInt(), random.nextInt()));
					break;

				case 1: // remove
					if (!list.isEmpty())
						list.remove(random.nextInt(list.size()));
					break;
				}
			}
		}
	}

	public class Set38 implements Job {
		private void randRemove(PSet1<Integer> set) {
			if (set.isEmpty())
				return;

			var index = random.nextInt(set.size());
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
				switch (random.nextInt(2)) {
				case 0: // insert
					set.add(random.nextInt());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public class Set39 implements Job {
		private void randRemove(PSet1<Long> set) {
			if (set.isEmpty())
				return;

			var index = random.nextInt(set.size());
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
				switch (random.nextInt(2)) {
				case 0: // insert
					set.add(random.nextLong());
					break;

				case 1: // remove
					randRemove(set);
					break;
				}
			}
		}
	}

	public class Map40 implements Job {

		@Override
		public void run(BValue value) {
			var map40 = value.getMap40(); // key is 0,1,2
			var forceRemove = map40.size() >= 3;
			if (forceRemove) {
				map40.remove(random.nextInt(3));
			} else {
				switch (random.nextInt(2)) {
				case 0: // put, maybe replace.
					map40.put(random.nextInt(3), random.nextInt());
					break;

				case 1: // remove, maybe remove nothing
					map40.remove(random.nextInt(3));
					break;
				}
			}
		}
	}

	public class Map41 implements Job {
		@Override
		public void run(BValue value) {
			var map41 = value.getMap41(); // key is 0,1,2
			var forceRemove = map41.size() >= 3;
			if (forceRemove) {
				map41.remove(random.nextLong(3));
			} else {
				switch (random.nextInt(3)) {
				case 0: // put, maybe replace.
					map41.put(random.nextLong(3), new BSimple());
					break;

				case 1: // remove, maybe remove nothing
					map41.remove(random.nextLong(3));
					break;

				case 2: // modify.
					if (map41.isEmpty())
						map41.put(random.nextLong(3), new BSimple()); // ensure modify
					var randItem = getRandItem(map41);
					assert randItem != null;
					randItem.setInt_1(random.nextInt());
					break;
				}
			}
		}

		private BSimple getRandItem(PMap2<Long, BSimple> map2) {
			if (map2.isEmpty())
				return null;
			var index = random.nextInt(map2.size());
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
			runJobs(rValue); // recursive, 没有退出限制，靠概率退出。
		}
	}

	// var 99 LongList 不测试了，跟List31一样。
}
