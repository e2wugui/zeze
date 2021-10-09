package Zeze.Util;

import Zeze.*;
import java.util.*;

public class SchedulerTask implements java.lang.Comparable<SchedulerTask> {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private Scheduler Scheduler;
	public final Scheduler getScheduler() {
		return Scheduler;
	}
	private void setScheduler(Scheduler value) {
		Scheduler = value;
	}
	private long Time;
	public final long getTime() {
		return Time;
	}
	private void setTime(long value) {
		Time = value;
	}
	private long Period;
	public final long getPeriod() {
		return Period;
	}
	private void setPeriod(long value) {
		Period = value;
	}
	private long SequenceNumber;
	public final long getSequenceNumber() {
		return SequenceNumber;
	}
	private void setSequenceNumber(long value) {
		SequenceNumber = value;
	}

	private volatile boolean canceled;
	private tangible.Action1Param<SchedulerTask> action;

	private static AtomicLong sequencer = new AtomicLong();

	public SchedulerTask(Scheduler scheduler, tangible.Action1Param<SchedulerTask> action, long initialDelay, long period) {
		this.setScheduler(scheduler);
		this.action = ::action;
		this.setTime(Zeze.Util.Time.getNowUnixMillis() + initialDelay);
		this.setPeriod(period);
		this.setSequenceNumber(sequencer.IncrementAndGet());
		this.canceled = false;
	}

	public final void Cancel() {
		this.canceled = true;
	}

	public final void Run() {
		if (this.canceled) {
			return;
		}

		// 派发出去运行，让系统管理大量任务的线程问题。
		Zeze.Util.Task.Run(() -> action.invoke(this), "SchedulerTask.Run");

		if (this.getPeriod() > 0) {
			this.setTime(this.getTime() + this.getPeriod());
			this.getScheduler().Scheule(this);
		}
	}

	public final int compareTo(SchedulerTask other) {
		if (other == null) { // 不可能吧
			return 1;
		}

		if (other == this) {
			return 0;
		}

		long diff = getTime() - other.getTime();
		if (diff < 0) {
			return -1;
		}

		if (diff > 0) {
			return 1;
		}

		if (this.getSequenceNumber() < other.getSequenceNumber()) {
			return -1;
		}

		return 1;
	}
}