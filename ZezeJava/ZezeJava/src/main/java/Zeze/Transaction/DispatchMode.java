package Zeze.Transaction;

public enum DispatchMode {
	Normal, // Task.run: Task.getThreadPool().execute
	Critical, // Task.getCriticalThreadPool().execute
	Direct, // Direct Call In Io Thread
}
