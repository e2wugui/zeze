package Zeze;

import Zeze.Net.Protocol;

public abstract class IModule {
	public abstract String getFullName();

	public abstract String getName();

	public abstract int getId();

	public String getWebPathBase() {
		return "";
	}

	public boolean isBuiltin() {
		return false;
	}

	public void Initialize(Zeze.AppBase app) {
	}

	public void UnRegister() { // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
	}

	@Deprecated //use errorCode
	public final long ErrorCode(int code) {
		return errorCode(code);
	}

	public final long errorCode(int code) {
		return errorCode(getId(), code);
	}

	public static long errorCode(int moduleId, int code) {
		if (code < 0)
			throw new IllegalArgumentException("code must greater than 0.");
		return Protocol.makeTypeId(moduleId, code);
	}

	public static int getModuleId(long result) {
		return Protocol.getModuleId(result);
	}

	public static int getErrorCode(long result) {
		return Protocol.getProtocolId(result);
	}
}
