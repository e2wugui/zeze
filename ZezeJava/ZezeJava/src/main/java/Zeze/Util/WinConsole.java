package Zeze.Util;

import java.util.function.IntPredicate;

public final class WinConsole {
	public static final int CTRL_C_EVENT = 0; // 命令行窗口中按Ctrl+C
	public static final int CTRL_BREAK_EVENT = 1; // 命令行窗口中按Ctrl+Break
	public static final int CTRL_CLOSE_EVENT = 2; // 点击命令行窗口的关闭按钮;对命令行窗口按ALT+F4快捷键;执行命令行窗口菜单的关闭命令
	public static final int CTRL_LOGOFF_EVENT = 5; // 系统注销时自动关闭非服务进程的命令行窗口;
	public static final int CTRL_SHUTDOWN_EVENT = 6; // 系统关机时自动关闭命令行窗口

	static {
		// if (System.getProperty("os.name").startsWith("Windows"))
		System.loadLibrary("WinConsole"); // 需要在当前目录存在: WinConsole.dll
	}

	/**
	 * 为命令行窗口的一些关闭事件增加自定义处理
	 *
	 * @param handler 自定义的处理器,传null表示取消之前加的处理器. 处理器的参数是事件枚举(见本类的CTRL_开头的常量定义),处理器返回是否继续执行原事件处理
	 * @return 是否执行成功
	 */
	public static native boolean hookCloseConsole(IntPredicate handler);

	/**
	 * 获取命令行窗口开始关闭事件后的处理超时时间, 超过此时间会被强杀进程
	 *
	 * @param event 处理器的参数是事件枚举(见本类的CTRL_开头的常量定义)
	 * @return 超时时间(毫秒). 无效event会返回小于0的值, 只支持CLOSE,LOGOFF,SHUTDOWN事件
	 */
	public static native int getCloseConsoleTimeout(int event);

	private WinConsole() {
	}

	public static void main(String[] args) throws InterruptedException {
		System.out.println(System.getProperty("os.name"));
		System.out.println("CTRL_CLOSE_EVENT timeout: " + getCloseConsoleTimeout(CTRL_CLOSE_EVENT));
		System.out.println("CTRL_LOGOFF_EVENT timeout: " + getCloseConsoleTimeout(CTRL_LOGOFF_EVENT));
		System.out.println("CTRL_SHUTDOWN_EVENT timeout: " + getCloseConsoleTimeout(CTRL_SHUTDOWN_EVENT));

		Runtime.getRuntime().addShutdownHook(new Thread("shutdownHook") {
			@Override
			public void run() {
				System.out.println("shutdown hook sleep begin");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					//noinspection CallToPrintStackTrace
					e.printStackTrace();
				}
				System.out.println("shutdown hook sleep end");
			}
		});

		var r = hookCloseConsole(event -> {
			System.out.println("hook event " + event + " in thread: " + Thread.currentThread().getName());
			if (event != CTRL_C_EVENT && event != CTRL_BREAK_EVENT)
				System.exit(event); // call shutdownHook then halt
			return false;
		});
		System.out.println("hook result: " + r);

		System.out.println("waiting ...");
		Thread.sleep(Long.MAX_VALUE);
	}
}
