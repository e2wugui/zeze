package Zeze.Arch;

/**
 * 按登录端点集合发送的函数接口。
 */
@FunctionalInterface
public interface OnlineSend {
	boolean send(Online.LoginOnLink logins);
}
