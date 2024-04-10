package SimpleRaft;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RaftTest {
	private final class Env implements Raft.Env {
		private final SimpleDateFormat dtf = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
		private final Date date = new Date(0);
		private final FieldPosition fieldPos = new FieldPosition(0);
		private final StringBuffer strBuf = new StringBuffer();
		private final ArrayDeque<Object> events = new ArrayDeque<>(16);
		private final ArrayDeque<Object> clientEvents = new ArrayDeque<>(16);
		private final HashMap<Long, Raft.Log> logMap = new HashMap<>();
		private Raft.Log @NotNull [] logs = new Raft.Log[4]; // 初始容量
		private long logCount; // logs的有效数量
		private long randSeed;
		private final int id;
		private final Raft raft;

		Env(int id) {
			this.id = id;
			randSeed = (id + 123456789L) * 987654321L;
			raft = new Raft(this, RAFT_COUNT, id, curTime);
		}

		@NotNull
		Raft getRaft() {
			return raft;
		}

		@Override
		public long getCurTime() {
			return curTime;
		}

		@Override
		public int random(int max) {
			long s = randSeed * 6364136223846793005L + 1442695040888963407L;
			randSeed = s;
			return (int)(((s >>> 32) * max) >>> 32);
		}

		@Override
		public @Nullable Object recvEvent() {
			return events.pollFirst();
		}

		@Override
		public void sendEvent(int id, @NotNull Object obj) {
			envs[id].events.addLast(obj);
		}

		@Nullable
		Object recvClientEvent() {
			return clientEvents.pollFirst();
		}

		@Override
		public void sendClientEvent(@NotNull Object obj) {
			clientEvents.addLast(obj);
		}

		@Override
		public long getLogCount() {
			return logCount;
		}

		@Override
		public Raft.Log getLog(long index) {
			if (index < 0)
				throw new IllegalArgumentException("index(" + index + ") < 0");
			if (index >= logCount)
				throw new IllegalArgumentException("index(" + index + ") >= logCount(" + logCount + ')');
			return logs[(int)index];
		}

		@Override
		public @NotNull Raft.Log @NotNull [] getLogs(long indexBegin, long indexEnd) {
			if (indexBegin < 0 || indexBegin > indexEnd)
				throw new IllegalArgumentException("invalid indexBegin=" + indexBegin + " or indexEnd=" + indexEnd);
			if (indexEnd > logCount)
				throw new IllegalArgumentException("indexEnd(" + indexEnd + ") > logCount(" + logCount + ')');
			return Arrays.copyOfRange(logs, (int)indexBegin, (int)indexEnd);
		}

		@Override
		public @Nullable Raft.Log getLogByKey(long key) {
			return null;
		}

		@Override
		public boolean appendLog(@NotNull Raft.Log log) {
			if (log.index != logCount)
				throw new IllegalArgumentException("log.index(" + log.index + ") != logCount(" + logCount + ')');
			if (logMap.containsKey(log.key))
				return false;
			long index = logCount;
			int n = logs.length;
			if (index >= n)
				logs = Arrays.copyOf(logs, n * 2);
			logs[(int)index] = log;
			logCount = index + 1;
			return true;
		}

		@Override
		public void appendLogs(@NotNull Raft.Log @NotNull [] logs) {
			int appendCount = logs.length;
			for (int i = 0; i < appendCount; i++)
				if (logs[i].index != logCount + i)
					throw new IllegalArgumentException("logs[" + i + "].index(" + logs[i].index + ") != logCount("
							+ logCount + '+' + i + ')');
			long index = logCount;
			long newCount = index + appendCount;
			int n = this.logs.length;
			if (newCount > n) {
				do
					n *= 2;
				while (newCount > n);
				this.logs = Arrays.copyOf(this.logs, n);
			}
			System.arraycopy(logs, 0, this.logs, (int)index, appendCount);
			logCount = newCount;
		}

		@Override
		public void truncateLogs(long logCount) {
			if (logCount < 0)
				throw new IllegalArgumentException("logCount(" + logCount + ") < 0");
			if (logCount > this.logCount)
				throw new IllegalArgumentException("logCount(" + logCount + ") > this.logCount(" + this.logCount + ')');
			for (int i = (int)logCount; i < this.logCount; i++)
				logMap.remove(logs[i].key);
			Arrays.fill(logs, (int)logCount, (int)this.logCount, null);
			this.logCount = logCount;
		}

		@Override
		public void traceInfo(@NotNull String info) {
			strBuf.setLength(0);
			date.setTime(System.currentTimeMillis());
			dtf.format(date, strBuf, fieldPos);
			System.out.println(strBuf.append(" [Raft-").append(id).append(':')
					.append(raft != null ? raft.getTerm() : -1).append("] [").append(curTime).append("] ")
					.append(info));
		}

		@Override
		public @NotNull String toString() {
			return "Env{" + "events=" + events + ", clientEvents=" + clientEvents + ", logs=" + Arrays.toString(logs)
					+ ", logCount=" + logCount + ", randSeed=" + randSeed + ", raft=" + raft + '}';
		}
	}

	private static final int RAFT_COUNT = 3;

	private final @NotNull Env[] envs = new Env[RAFT_COUNT];
	private final HashSet<Long> sendingSerials = new HashSet<>();
	private long curTime;
	private long serialCounter;
	private int leftSendAddLogCount;
	private int concurrentSendCount;

	private RaftTest() {
		for (int i = 0; i < RAFT_COUNT; i++)
			envs[i] = new Env(i);
	}

	private int getLeaderId() {
		for (Env env : envs)
			if (env.getRaft().getLeaderId() == env.id)
				return env.id;
		return -1;
	}

	private void trySendAddLog() {
		while (leftSendAddLogCount > 0 && sendingSerials.size() < concurrentSendCount) {
			int leaderId = getLeaderId();
			if (leaderId < 0)
				return;
			leftSendAddLogCount--;
			sendingSerials.add(++serialCounter);
			envs[leaderId].sendEvent(leaderId, new Raft.AddLog(serialCounter, new Raft.Log(serialCounter)));
		}
	}

	private void runRaftServer(int time) {
		for (long endTime = curTime + time; curTime < endTime; curTime += 1) {
			trySendAddLog();
			for (Env env : envs) {
				env.getRaft().run();
				for (Object event; (event = env.recvClientEvent()) != null; ) {
					env.traceInfo("recvClientEvent: " + event);
					Raft.AddLogRe re = (Raft.AddLogRe)event;
					sendingSerials.remove(re.serial);
					if (re.result < 0)
						throw new IllegalStateException("AddLogRe.result=" + re.result);
				}
			}
		}
	}

	public void testSimple() {
		runRaftServer(1_000);

		leftSendAddLogCount += 10;
		concurrentSendCount = 4;

		runRaftServer(2_000);

		for (Env env : envs)
			env.traceInfo("TEST END: " + env);

		if (!sendingSerials.isEmpty())
			throw new IllegalStateException("sendingSerials.size=" + sendingSerials.size());
		if (leftSendAddLogCount != 0)
			throw new IllegalStateException("leftSendAddLogCount=" + leftSendAddLogCount);
		for (Env env : envs) {
			if (env.getRaft().getCommitIndex() != serialCounter - 1)
				throw new IllegalStateException("unmatched commitIndex for id=" + env.id);
		}
		System.out.println("CHECK OK!");
	}

	public static void main(String[] args) {
		new RaftTest().testSimple();
	}
}
