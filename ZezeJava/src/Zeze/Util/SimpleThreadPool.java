package Zeze.Util;

import Zeze.*;
import java.util.*;

public final class SimpleThreadPool {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private BlockingCollection<tangible.Action0Param> taskQueue = new BlockingCollection<tangible.Action0Param>();
	private ArrayList<Thread> workers = new ArrayList<Thread>();
	private String Name;
	public String getName() {
		return Name;
	}

	public boolean QueueUserWorkItem(tangible.Action0Param action) {
		if (false == taskQueue.IsAddingCompleted) {
			taskQueue.Add(action);
		}
		// skip task
		return true;
	}

	public void Shutdown() {
		taskQueue.CompleteAdding();

		synchronized (this) {
			while (taskQueue.IsCompleted == false) {
				Monitor.Wait(this);
			}
		}
	}

	public SimpleThreadPool(int workerThreads, String poolName) {
		Name = poolName;

		for (int i = 0; i < workerThreads; ++i) {
			workers.add(new Thread() {
			void run() {
				MainRun();
			}
			};
		}

		for (Thread thread : workers) {
			thread.start();
		}

	}

	private void MainRun() {
		while (true) {
			tangible.Action0Param action = ::null;
			try {
				if (taskQueue.IsCompleted) {
					synchronized (this) {
						Monitor.PulseAll(this);
						break;
					}
				}
				action = taskQueue.Take();
				if (null == action) {
					break;
				}
				action.invoke();
			}
			/*
			catch (OperationCanceledException)
			{
			    // skip
			}
			*/
			catch (RuntimeException ex) {
				logger.Error(ex, "SimpleThreadPool {0}", action);
			}
		}
	}
}