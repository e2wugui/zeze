package Zeze.Util;

import java.io.Serial;

public class TaskCanceledException extends Error {
	@Serial
	private static final long serialVersionUID = -1047347523279541091L;

	public TaskCanceledException() {
	}

	public TaskCanceledException(String msg) {
		super(msg);
	}
}
