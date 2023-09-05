package Zeze;

import Zeze.Net.Protocol;

public interface IModule {
	String getFullName();

	String getName();

	int getId();

	default String getWebPathBase() {
		return "";
	}

	default boolean isBuiltin() {
		return false;
	}

	default void StartLast() throws Exception {
	}

	default void Initialize(AppBase app) throws Exception {
	}

	default void UnRegister() { // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
	}

	default long errorCode(int code) {
		return errorCode(getId(), code);
	}

	static long errorCode(int moduleId, int code) {
		if (code < 0)
			throw new IllegalArgumentException("code must greater than 0.");
		return Protocol.makeTypeId(moduleId, code);
	}

	static int getModuleId(long result) {
		return Protocol.getModuleId(result);
	}

	static int getErrorCode(long result) {
		return Protocol.getProtocolId(result);
	}
}
