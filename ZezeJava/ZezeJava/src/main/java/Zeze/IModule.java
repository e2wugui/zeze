package Zeze;

import Zeze.Net.Protocol;

public interface IModule {
	public String getFullName();

	public String getName();

	public int getId();

	default String getWebPathBase() {
		return "";
	}

	default boolean isBuiltin() {
		return false;
	}

	default void Initialize(Zeze.AppBase app) {
	}

	default void UnRegister() { // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
	}

	@Deprecated //use errorCode
	default long ErrorCode(int code) {
		return errorCode(code);
	}

	default long errorCode(int code) {
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
