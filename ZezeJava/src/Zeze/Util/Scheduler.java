package Zeze.Util;

import Zeze.*;
import java.util.*;

/** 
 定时延期执行任务。有 System.Threading.Timer，这个没必要了。
*/
public final class Scheduler {
	private TreeMap<SchedulerTask, SchedulerTask> scheduled = new TreeMap<SchedulerTask, SchedulerTask>();
	private Thread thread;
	private volatile boolean isRunning;

	private static Scheduler Instance = new Scheduler();
	public static Scheduler getInstance() {
		return Instance;
	}

	public Scheduler() {
		isRunning = true;
		thread = new Thread() {
		void run() {
			ThreadRun();
		}
		};
		thread.IsBackground = true;
		thread.start();
	}

	/** 
	 调度一个执行 action。
	 
	 @param action
	 @param initialDelay the time to delay first execution. Milliseconds
	 @param period the period between successive executions. Milliseconds
	 @return 
	*/

	public SchedulerTask Schedule(Action<SchedulerTask> action, long initialDelay) {
		return Schedule(action, initialDelay, -1);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public SchedulerTask Schedule(Action<SchedulerTask> action, long initialDelay, long period = -1)
	public SchedulerTask Schedule(tangible.Action1Param<SchedulerTask> action, long initialDelay, long period) {
		synchronized (this) {
			if (initialDelay < 0) {
				throw new IllegalArgumentException();
			}

			SchedulerTask t = new SchedulerTask(this, action, initialDelay, period);
			scheduled.put(t, t);
			System.Threading.Monitor.Pulse(this);
			return t;
		}
	}

	/** 
	 设置停止标志，并等待调度线程结束。不是必须调用。
	*/
	public void StopAndJoin() {
		isRunning = false;
		synchronized (this) {
			System.Threading.Monitor.Pulse(this);
		}
		thread.join();
	}

	public void Scheule(SchedulerTask t) {
		synchronized (this) {
			scheduled.put(t, t);
			System.Threading.Monitor.Pulse(this);
		}
	}

	private void ThreadRun() {
		while (isRunning) {
			ArrayList<SchedulerTask> willRun = new ArrayList<SchedulerTask>(scheduled.size());
			long nextTime = -1;
			long now = Time.getNowUnixMillis();

			synchronized (this) {
				for (SchedulerTask k : scheduled.keySet()) {
					if (k.getTime() <= now) {
						willRun.add(k);
						continue;
					}
					nextTime = k.getTime();
					break;
				}
				for (SchedulerTask k : willRun) {
					scheduled.remove(k);
				}
			}

			for (SchedulerTask k : willRun) {
				k.Run();
			}

			if (!willRun.isEmpty()) { // 如果执行了任务，可能有重新调度的Task，马上再次检测。
				continue;
			}

			synchronized (this) {
				int waitTime = System.Threading.Timeout.Infinite;
				if (nextTime > now) {
					waitTime = (int)(nextTime - now);
				}
				System.Threading.Monitor.Wait(this, waitTime); // wait until new task or nextTime.
			}
		}
	}
}