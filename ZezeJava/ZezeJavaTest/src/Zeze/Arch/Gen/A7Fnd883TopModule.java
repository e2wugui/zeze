package Zeze.Arch.Gen;

import Zeze.AppBase;
import Zeze.Arch.RedirectToServer;

/**
 * FND8-83文件模式试编译fixture：顶层模块类。嵌套fixture类的二进制名（Outer$Inner）
 * 进生成源码的extends子句后javac无法解析，无法验证"试编译通过"的正路径，故用顶层形态。
 */
public class A7Fnd883TopModule implements Zeze.IModule {
	public static final int ModuleId = 8831;
	public static final String ModuleFullName = "Zeze.Arch.Gen.A7Fnd883TopModule";

	public A7Fnd883TopModule(AppBase app) {
	}

	@Override
	public String getFullName() {
		return ModuleFullName;
	}

	@Override
	public String getName() {
		return "A7Fnd883TopModule";
	}

	@Override
	public int getId() {
		return ModuleId;
	}

	@RedirectToServer
	public void ping(int serverId) {
	}
}
