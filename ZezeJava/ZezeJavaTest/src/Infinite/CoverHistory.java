package Infinite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import Zeze.Net.Binary;
import Zeze.Transaction.Collections.PSet1;
import Zeze.Util.Task;
import demo.Bean1;
import demo.Module1.BValue;
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
				if (null == r)
					r = app.demo_Module1.getTableCoverHistory().getOrAdd(random.nextLong(eKeyRange));
				jobs.get(index).run(r);
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
			var bytes = new byte[16];
			random.nextBytes(bytes);
			value.setBytes8(new Binary(bytes));
		}
	}

	public class List9 implements Job {
		@Override
		public void run(BValue value) {
			var list = value.getList9();
			var forceRemove = list.size() >= 10;
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
			var forceRemove = set.size() >= 10;
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
}
