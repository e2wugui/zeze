package Zeze.Util;

public class TaskCanceledException extends RuntimeException {
	static final long serialVersionUID = 1633764672881L;

	public TaskCanceledException() {
		
	}
	
	public TaskCanceledException(String msg) {
		super(msg);		
	}
}
