package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Raft.RocksRaft.Log1.LogLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.SimpleAssert;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;

public final class Test {
	public static final class Bean1 extends Bean {
		private int _i;
		private long _l;
		private final CollMap1<Integer, Integer> _map1;
		private final Bean2 _bean2;
		private final CollMap2<Integer, Bean1> _map2;

		private transient Object _Int32MapKey_;

		@Override
		public Object mapKey() {
			return _Int32MapKey_;
		}

		@Override
		public void mapKey(Object mapKey) {
			_Int32MapKey_ = mapKey;
		}

		public int getI() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _i;
				var log = Transaction.getCurrent().getLog(objectId() + 1);
				if (log == null)
					return _i;
				return ((LogInt)log).value;
			}
			return _i;
		}

		public void setI(int value) {
			if (isManaged())
				Transaction.getCurrent().putLog(new LogInt(this, 1, value));
			else
				_i = value;
		}

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		public long getL() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _l;
				var log = Transaction.getCurrent().getLog(objectId() + 2);
				if (log == null)
					return _l;
				return ((LogLong)log).value;
			}
			return _l;
		}

		public void setL(long value) {
			if (isManaged())
				Transaction.getCurrent().putLog(new LogLong(this, 2, value));
			else
				_l = value;
		}

		public CollMap1<Integer, Integer> getMap1() {
			return _map1;
		}

		public CollMap2<Integer, Bean1> getMap2() {
			return _map2;
		}

		public Bean2 getBean2() {
			return _bean2;
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				switch (vlog.getVariableId()) {
				case 1:
					_i = ((LogInt)vlog).value;
					break;
				case 2:
					_l = ((LogLong)vlog).value;
					break;
				case 3:
					_map1.followerApply(vlog);
					break;
				case 4:
					_bean2.followerApply(vlog);
					break;
				case 5:
					_map2.followerApply(vlog);
					break;
				}
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			switch (vlog.getVariableId()) {
			case 1:
				_i = ((LogInt)vlog).value;
				break;
			case 2:
				_l = ((LogLong)vlog).value;
				break;
			case 3:
				_map1.leaderApplyNoRecursive(vlog);
				break;
			case 5:
				_map2.leaderApplyNoRecursive(vlog);
				break;
			}
		}

		public Bean1() {
			_map1 = new CollMap1<>(Integer.class, Integer.class);
			_map1.variableId(3);
			_bean2 = new Bean2();
			_bean2.variableId(4);
			_map2 = new CollMap2<>(Integer.class, Bean1.class);
			_map2.variableId(5);
		}

		@Override
		public void decode(IByteBuffer bb) {
			_Int32MapKey_ = bb.ReadInt();

			setI(bb.ReadInt());
			setL(bb.ReadLong());
			getMap1().decode(bb);
			getBean2().decode(bb);
			getMap2().decode(bb);
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(_Int32MapKey_ != null ? (Integer)_Int32MapKey_ : 0);

			bb.WriteInt(getI());
			bb.WriteLong(getL());
			getMap1().encode(bb);
			getBean2().encode(bb);
			getMap2().encode(bb);
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
			_map1.initRootInfo(root, this);
			_bean2.initRootInfo(root, this);
			_map2.initRootInfo(root, this);
		}

		@Override
		public String toString() {
			return String.format("Bean1(%s I=%s L=%s Map1=%s Bean2=%s Map2=%s)",
					_Int32MapKey_ != null ? _Int32MapKey_ : 0, getI(), getL(), getMap1(), getBean2(), getMap2());
		}
	}

	public static final class Bean2 extends Bean {
		private int _i;

		@Override
		public Bean copy() {
			throw new UnsupportedOperationException();
		}

		public int getI() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _i;
				var log = Transaction.getCurrent().getLog(objectId() + 1);
				if (log == null)
					return _i;
				return ((LogInt)log).value;
			}
			return _i;
		}

		public void setI(int value) {
			if (isManaged())
				Transaction.getCurrent().putLog(new LogInt(this, 1, value));
			else
				_i = value;
		}

		@Override
		public void decode(IByteBuffer bb) {
			setI(bb.ReadInt());
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(getI());
		}

		@Override
		protected void initChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public String toString() {
			return String.format("Bean2(I=%d)", getI());
		}

		@Override
		public void followerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				//noinspection SwitchStatementWithTooFewBranches
				switch (vlog.getVariableId()) {
				case 1:
					_i = ((LogInt)vlog).value;
					break;
				}
			}
		}

		@Override
		public void leaderApplyNoRecursive(Log vlog) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (vlog.getVariableId()) {
			case 1:
				_i = ((LogInt)vlog).value;
				break;
			}
		}
	}

	private static void remove1(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			table.remove(1);

			Transaction.getCurrent().runWhileCommit(() ->
			{
				var c = Transaction.getCurrent().getChanges();
				SimpleAssert.isTrue(c.getBeans().size() == 1);
				SimpleAssert.isTrue(c.getRecords().size() == 1);

				var r = c.getRecords().get(new TableKey(table.getName(), 1));
				SimpleAssert.isTrue(null != r);
				SimpleAssert.isNull(r.getPutValue());
				SimpleAssert.areEqual(Changes.Record.Remove, r.getState());
				SimpleAssert.areEqual(1, r.getLogBeans().size());
				SimpleAssert.areEqual(0, r.getLogBean().size());
			});
			return 0L;
		}).call();
	}

	private static void update(Table<Integer, Bean1> table, int num) {
		var value = table.getOrAdd(1);

		// 本层Bean变量修改日志
		value.setI(1 + num);

		// 下一层Bean变量修改日志
		value.getBean2().setI(2 + num);

		// 本层Bean容器变量修改日志
		value.getMap1().put(3 + num, 3 + num);

		// 本层Bean容器变量修改日志2
		var bean1 = new Bean1();
		value.getMap2().put(4 + num, bean1);

		// 容器内Bean修改日志。
		bean1.setI(5 + num);
	}

	private static void verifyChanges(String expected) {
		Transaction.getCurrent().runWhileCommit(() -> {
			var Changes = Transaction.getCurrent().getChanges();
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Changes.getRecords());
			if (expected == null || expected.isEmpty())
				System.out.println(sb);
			else
				SimpleAssert.areEqual(expected.replace("\r", ""), sb.toString());
		});
	}

	private static void verifyData(Rocks rocks, Table<Integer, Bean1> table, String expected) throws Exception {
		rocks.newProcedure(() -> {
			var value = table.getOrAdd(1);
			var current = value.toString();
			if (expected == null)
				System.out.println(current);
			else
				SimpleAssert.areEqual(expected, current);
			return 0L;
		}).call();
	}

	private static void putAndEdit(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			update(table, 0);
			verifyChanges("{(tRocksRaft#0,1):State=1 PutValue=Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})\n" +
					"Log=[]\n" +
					"AllLog=[{0:Value=Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})},{1:Value=1,3: Putted:{3:3} Removed:[],4:{1:Value=2},5: Putted:{4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]}");
			return 0L;
		}).call();
	}

	private static void edit(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			update(table, 10);
			verifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]\n" +
					"AllLog=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]}");
			return 0L;
		}).call();
	}

	private static void editAndPut(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			update(table, 20);
			// 重新put，将会让上面的修改树作废。但所有的日志树都可以从All中看到。
			var bean1put = new Bean1();
			table.put(1, bean1put);
			verifyChanges("{(tRocksRaft#0,1):State=1 PutValue=Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})\n" +
					"Log=[]\n" +
					"AllLog=[{0:Value=Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})},{1:Value=21,3: Putted:{23:23} Removed:[],4:{1:Value=22},5: Putted:{24:Bean1(24 I=25 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=25}]}]}");
			return 0L;
		}).call();
	}

	private static void editInContainer(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			var value = table.getOrAdd(1);
			var edit = value.getMap2().get(14);
			edit.getBean2().setI(2222);
			verifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]\n" +
					"AllLog=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]}");
			return 0L;
		}).call();
	}

	private static void nestProcedure(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			var value = table.get(1);
			value.getBean2().setI(3333);

			rocks.newProcedure(() ->
			{
				var value2 = table.get(1);
				value2.getBean2().setI(4444);
				SimpleAssert.areEqual(4444, value2.getBean2().getI());
				return -1L;
			}).call();

			verifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{4:{1:Value=3333}}]\n" +
					"AllLog=[{4:{1:Value=3333}}]}");
			return 0L;
		}).call();
	}

	private static void nestProcedureContainer(Rocks rocks, Table<Integer, Bean1> table) throws Exception {
		rocks.newProcedure(() -> {
			rocks.newProcedure(() -> {
				var value = table.get(1);
				value.getMap2().put(4444, new Bean1());
				value.getMap1().put(4444, 4444);
				value.getMap1().remove(3);
				value.getMap2().remove(4);
				return 0L;
			}).call();

			verifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]\n" +
					"AllLog=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]}");
			return 0L;
		}).call();
	}

	private static Rocks getLeader(List<Rocks> rocks, Rocks skip) throws InterruptedException {
		while (true) {
			for (var rock : rocks) {
				if (rock != skip && rock.isLeader())
					return rock;
			}
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	public static void test_1() throws Exception {
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6000"));
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6001"));
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6002"));

		Rocks.registerLog(() -> new LogMap1<>(Integer.class, Integer.class));
		Rocks.registerLog(() -> new LogMap2<>(Integer.class, Bean1.class));

		try (var rocks1 = new Rocks("127.0.0.1:6000");
			 var rocks2 = new Rocks("127.0.0.1:6001");
			 var rocks3 = new Rocks("127.0.0.1:6002")) {
			var rocksList = List.of(rocks1, rocks2, rocks3);
			for (var rr : rocksList)
				rr.registerTableTemplate("tRocksRaft", Integer.class, Bean1.class);

			rocks1.getRaft().getServer().start();
			rocks2.getRaft().getServer().start();
			rocks3.getRaft().getServer().start();

			var leader = getLeader(rocksList, null);
			runLeader(leader);
			leader.getRaft().getServer().stop();

			// 只简单验证一下最新的数据。
			var newLeader = getLeader(rocksList, leader);
			verifyData(newLeader, newLeader.<Integer, Bean1>getTableTemplate("tRocksRaft")
					.openTable(0), "Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");
		}
	}

	private static void runLeader(Rocks rocks) throws Exception {
		var table = rocks.<Integer, Bean1>getTableTemplate("tRocksRaft").openTable(0);
		remove1(rocks, table);

		putAndEdit(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		edit(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		editInContainer(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

		nestProcedure(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=3333) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

		nestProcedureContainer(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={13:13,4444:4444} Bean2=Bean2(I=3333) Map2={14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={}),4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		editAndPut(rocks, table);
		verifyData(rocks, table, "Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");

		// 再次运行本测试，才会执行到 LoadSnapshot。
		// rocks.getRaft().getLogSequence().Snapshot(true);
	}

	public static void main(String[] args) throws Exception {
		Task.initThreadPool(Task.newFixedThreadPool(5, "test"),
				Executors.newScheduledThreadPool(3, new ThreadFactoryWithName("test-sch")));
		test_1();
		System.out.println("main end!");
	}
}
