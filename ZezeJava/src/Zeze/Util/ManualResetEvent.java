package Zeze.Util;

public class ManualResetEvent {
	private final Object monitor = new Object();
	private volatile boolean open;

	public ManualResetEvent(boolean open) {
		this.open = open;
	}

	public void WaitOne() throws InterruptedException {
		synchronized (monitor) {
			while (!open) {
				monitor.wait();
			}
		}
	}

	public boolean WaitOne(long milliseconds) {
		synchronized (monitor) {
			if (open)
				return true;
			try {
				monitor.wait(milliseconds);				
			}
			catch (Throwable ex) {
				throw new RuntimeException(ex);
			}
			return open;
		}
	}

	public void Set() {// open start
		synchronized (monitor) {
			open = true;
			monitor.notifyAll();
		}
	}

	public void Reset() {// close stop
		open = false;
	}
}
