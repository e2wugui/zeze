package Zeze.Raft.RocksRaft;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RocksRaft.Log1.LogInt;
import Zeze.Raft.RocksRaft.Log1.LogLong;
import Zeze.Serialize.ByteBuffer;
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
		public Object getMapKey() {
			return _Int32MapKey_;
		}

		@Override
		public void setMapKey(Object mapKey) {
			_Int32MapKey_ = mapKey;
		}

		public int getI() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _i;
				var log = Transaction.getCurrent().GetLog(getObjectId() + 1);
				if (log == null)
					return _i;
				return ((LogInt)log).Value;
			}
			return _i;
		}

		public void setI(int value) {
			if (isManaged())
				Transaction.getCurrent().PutLog(new LogInt(this, 1, value));
			else
				_i = value;
		}

		@Override
		public Bean CopyBean() {
			throw new UnsupportedOperationException();
		}

		public long getL() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _l;
				var log = Transaction.getCurrent().GetLog(getObjectId() + 2);
				if (log == null)
					return _l;
				return ((LogLong)log).Value;
			}
			return _l;
		}

		public void setL(long value) {
			if (isManaged())
				Transaction.getCurrent().PutLog(new LogLong(this, 2, value));
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
		public void FollowerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				switch (vlog.getVariableId()) {
				case 1:
					_i = ((LogInt)vlog).Value;
					break;
				case 2:
					_l = ((LogLong)vlog).Value;
					break;
				case 3:
					_map1.FollowerApply(vlog);
					break;
				case 4:
					_bean2.FollowerApply(vlog);
					break;
				case 5:
					_map2.FollowerApply(vlog);
					break;
				}
			}
		}

		@Override
		public void LeaderApplyNoRecursive(Log vlog) {
			switch (vlog.getVariableId()) {
			case 1:
				_i = ((LogInt)vlog).Value;
				break;
			case 2:
				_l = ((LogLong)vlog).Value;
				break;
			case 3:
				_map1.LeaderApplyNoRecursive(vlog);
				break;
			case 5:
				_map2.LeaderApplyNoRecursive(vlog);
				break;
			}
		}

		public Bean1() {
			_map1 = new CollMap1<>(Integer.class, Integer.class);
			_map1.setVariableId(3);
			_bean2 = new Bean2();
			_bean2.setVariableId(4);
			_map2 = new CollMap2<>(Integer.class, Bean1.class);
			_map2.setVariableId(5);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			_Int32MapKey_ = bb.ReadInt();

			setI(bb.ReadInt());
			setL(bb.ReadLong());
			getMap1().Decode(bb);
			getBean2().Decode(bb);
			getMap2().Decode(bb);
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(_Int32MapKey_ != null ? (Integer)_Int32MapKey_ : 0);

			bb.WriteInt(getI());
			bb.WriteLong(getL());
			getMap1().Encode(bb);
			getBean2().Encode(bb);
			getMap2().Encode(bb);
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
			_map1.InitRootInfo(root, this);
			_bean2.InitRootInfo(root, this);
			_map2.InitRootInfo(root, this);
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
		public Bean CopyBean() {
			throw new UnsupportedOperationException();
		}

		public int getI() {
			if (isManaged()) {
				if (Transaction.getCurrent() == null)
					return _i;
				var log = Transaction.getCurrent().GetLog(getObjectId() + 1);
				if (log == null)
					return _i;
				return ((LogInt)log).Value;
			}
			return _i;
		}

		public void setI(int value) {
			if (isManaged())
				Transaction.getCurrent().PutLog(new LogInt(this, 1, value));
			else
				_i = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setI(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(getI());
		}

		@Override
		protected void InitChildrenRootInfo(Record.RootInfo root) {
		}

		@Override
		public String toString() {
			return String.format("Bean2(I=%d)", getI());
		}

		@Override
		public void FollowerApply(Log log) {
			var vars = ((LogBean)log).getVariables();
			if (vars == null)
				return;
			for (var it = vars.iterator(); it.moveToNext(); ) {
				var vlog = it.value();
				//noinspection SwitchStatementWithTooFewBranches
				switch (vlog.getVariableId()) {
				case 1:
					_i = ((LogInt)vlog).Value;
					break;
				}
			}
		}

		@Override
		public void LeaderApplyNoRecursive(Log vlog) {
			//noinspection SwitchStatementWithTooFewBranches
			switch (vlog.getVariableId()) {
			case 1:
				_i = ((LogInt)vlog).Value;
				break;
			}
		}
	}

	private static void Remove1(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			table.Remove(1);

			Transaction.getCurrent().RunWhileCommit(() ->
			{
				var c = Transaction.getCurrent().getChanges();
				SimpleAssert.IsTrue(c.getBeans().size() == 1);
				SimpleAssert.IsTrue(c.getRecords().size() == 1);

				var r = c.getRecords().get(new TableKey(table.getName(), 1));
				SimpleAssert.IsTrue(null != r);
				SimpleAssert.IsNull(r.getPutValue());
				SimpleAssert.AreEqual(Changes.Record.Remove, r.getState());
				SimpleAssert.IsTrue(r.getLogBeans().size() == 1);
				SimpleAssert.IsTrue(r.getLogBean().size() == 0);
			});
			return 0L;
		}).Call();
	}

	private static void Update(Table<Integer, Bean1> table, int num) {
		var value = table.GetOrAdd(1);

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

	private static void VerifyChanges(String expected) {
		Transaction.getCurrent().RunWhileCommit(() -> {
			var Changes = Transaction.getCurrent().getChanges();
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Changes.getRecords());
			if (expected == null || expected.isEmpty())
				System.out.println(sb);
			else
				SimpleAssert.AreEqual(expected.replace("\r", ""), sb.toString());
		});
	}

	private static void VerifyData(Rocks rocks, Table<Integer, Bean1> table, String expected) throws Throwable {
		rocks.NewProcedure(() -> {
			var value = table.GetOrAdd(1);
			var current = value.toString();
			if (expected == null)
				System.out.println(current);
			else
				SimpleAssert.AreEqual(expected, current);
			return 0L;
		}).Call();
	}

	private static void PutAndEdit(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			Update(table, 0);
			VerifyChanges("{(tRocksRaft#0,1):State=1 PutValue=Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})\n" +
					"Log=[]\n" +
					"AllLog=[{0:Value=Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})},{1:Value=1,3: Putted:{3:3} Removed:[],4:{1:Value=2},5: Putted:{4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=5}]}]}");
			return 0L;
		}).Call();
	}

	private static void Edit(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			Update(table, 10);
			VerifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]\n" +
					"AllLog=[{1:Value=11,3: Putted:{13:13} Removed:[],4:{1:Value=12},5: Putted:{14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=15}]}]}");
			return 0L;
		}).Call();
	}

	private static void EditAndPut(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			Update(table, 20);
			// 重新put，将会让上面的修改树作废。但所有的日志树都可以从All中看到。
			var bean1put = new Bean1();
			table.Put(1, bean1put);
			VerifyChanges("{(tRocksRaft#0,1):State=1 PutValue=Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})\n" +
					"Log=[]\n" +
					"AllLog=[{0:Value=Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})},{1:Value=21,3: Putted:{23:23} Removed:[],4:{1:Value=22},5: Putted:{24:Bean1(24 I=25 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[] Changed:[{1:Value=25}]}]}");
			return 0L;
		}).Call();
	}

	private static void EditInContainer(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			var value = table.GetOrAdd(1);
			var edit = value.getMap2().get(14);
			edit.getBean2().setI(2222);
			VerifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]\n" +
					"AllLog=[{5: Putted:{} Removed:[] Changed:[{4:{1:Value=2222}}]}]}");
			return 0L;
		}).Call();
	}

	private static void NestProcedure(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			var value = table.Get(1);
			value.getBean2().setI(3333);

			rocks.NewProcedure(() ->
			{
				var value2 = table.Get(1);
				value2.getBean2().setI(4444);
				SimpleAssert.AreEqual(4444, value2.getBean2().getI());
				return -1L;
			}).Call();

			VerifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{4:{1:Value=3333}}]\n" +
					"AllLog=[{4:{1:Value=3333}}]}");
			return 0L;
		}).Call();
	}

	private static void NestProcedureContainer(Rocks rocks, Table<Integer, Bean1> table) throws Throwable {
		rocks.NewProcedure(() -> {
			rocks.NewProcedure(() -> {
				var value = table.Get(1);
				value.getMap2().put(4444, new Bean1());
				value.getMap1().put(4444, 4444);
				value.getMap1().remove(3);
				value.getMap2().remove(4);
				return 0L;
			}).Call();

			VerifyChanges("{(tRocksRaft#0,1):State=2 PutValue=null\n" +
					"Log=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]\n" +
					"AllLog=[{3: Putted:{4444:4444} Removed:[3],5: Putted:{4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})} Removed:[4] Changed:[]}]}");
			return 0L;
		}).Call();
	}

	private static Rocks GetLeader(List<Rocks> rocks, Rocks skip) throws InterruptedException {
		while (true) {
			for (var rock : rocks) {
				if (rock != skip && rock.isLeader())
					return rock;
			}
			//noinspection BusyWait
			Thread.sleep(100);
		}
	}

	public static void Test_1() throws Throwable {
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6000"));
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6001"));
		LogSequence.deletedDirectoryAndCheck(new File("127.0.0.1_6002"));

		Rocks.RegisterLog(() -> new LogMap1<>(Integer.class, Integer.class));
		Rocks.RegisterLog(() -> new LogMap2<>(Integer.class, Bean1.class));

		try (var rocks1 = new Rocks("127.0.0.1:6000");
			 var rocks2 = new Rocks("127.0.0.1:6001");
			 var rocks3 = new Rocks("127.0.0.1:6002")) {
			var rocksList = List.of(rocks1, rocks2, rocks3);
			for (var rr : rocksList)
				rr.RegisterTableTemplate("tRocksRaft", Integer.class, Bean1.class);

			rocks1.getRaft().getServer().Start();
			rocks2.getRaft().getServer().Start();
			rocks3.getRaft().getServer().Start();

			var leader = GetLeader(rocksList, null);
			RunLeader(leader);
			leader.getRaft().getServer().Stop();

			// 只简单验证一下最新的数据。
			var newLeader = GetLeader(rocksList, leader);
			VerifyData(newLeader, newLeader.<Integer, Bean1>GetTableTemplate("tRocksRaft")
					.OpenTable(0), "Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");
		}
	}

	private static void RunLeader(Rocks rocks) throws Throwable {
		var table = rocks.<Integer, Bean1>GetTableTemplate("tRocksRaft").OpenTable(0);
		Remove1(rocks, table);

		PutAndEdit(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=1 L=0 Map1={3:3} Bean2=Bean2(I=2) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		Edit(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		EditInContainer(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=12) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

		NestProcedure(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={3:3,13:13} Bean2=Bean2(I=3333) Map2={4:Bean1(4 I=5 L=0 Map1={} Bean2=Bean2(I=0) Map2={}),14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={})})");

		NestProcedureContainer(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=11 L=0 Map1={13:13,4444:4444} Bean2=Bean2(I=3333) Map2={14:Bean1(14 I=15 L=0 Map1={} Bean2=Bean2(I=2222) Map2={}),4444:Bean1(4444 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})})");

		EditAndPut(rocks, table);
		VerifyData(rocks, table, "Bean1(0 I=0 L=0 Map1={} Bean2=Bean2(I=0) Map2={})");

		// 再次运行本测试，才会执行到 LoadSnapshot。
		// rocks.getRaft().getLogSequence().Snapshot(true);
	}

	public static void main(String[] args) throws Throwable {
		Task.initThreadPool(Task.newFixedThreadPool(5, "test"),
				Executors.newScheduledThreadPool(3, new ThreadFactoryWithName("test-sch")));
		Test_1();
		System.out.println("main end!");
	}
}
