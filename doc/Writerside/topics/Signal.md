# Signal

## 安全结束(SIGHUP(1), SIGINT(2), SIGTERM(15))
* Java程序: System.exit(...)
* Linux控制台: kill -1 {PID}; kill -2 {PID}; kill -15 {PID}
* Windows控制台: 运行windows-kill或sendsignal程序
* Linux/Windows控制台: 按键盘Ctrl+C

## 强制结束(SIGKILL(9))
* Java程序: Runtime.getRuntime().halt(...)
* Linux控制台: kill -9 {PID}
* Windows控制台: taskkill /f /pid {PID}
* Windows任务管理器: 结束任务/结束进程
* Windows控制台窗口: 点关闭按钮,执行菜单中的关闭命令(可使用Zeze.Util.WinConsole.hookCloseConsole(...)拦截处理)
* IDEA的运行框: 点停止按钮,执行菜单中的Stop命令

## Java程序输出栈信息到错误流(SIGQUIT(3))
* Linux控制台: kill -3 {PID}
* Windows控制台: 运行windows-kill程序
* Windows控制台: 按键盘Ctrl+Break

## 参考
* sendsignal: https://github.com/walware/statet/blob/master/de.walware.statet.r.console.core/cppSendSignal/sendsignal.cpp
* windows-kill: https://github.com/ElyDotDev/windows-kill
