package Zeze.Util;

import java.io.Serial;

// 任务取消专用异常（Error 形态）：框架按错误码 Procedure.CancelException 识别处理
public class TaskCanceledException extends Error {
	@Serial private static final long serialVersionUID = -1047347523279541091L;

	public TaskCanceledException() {
	}

	public TaskCanceledException(String msg) {
		super(msg);
	}
}
