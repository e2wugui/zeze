package Zeze;

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

	public final long ErrorCode(int code) {
		return ErrorCode(getId(), code);
	}

	public static long ErrorCode(int moduleId, int code) {
		if (code < 0)
			throw new IllegalArgumentException("code must greater than 0.");
		return Zeze.Net.Protocol.MakeTypeId(moduleId, code);
	}

	public static int GetModuleId(long result) {
		return Zeze.Net.Protocol.getModuleId(result);
	}

	public static int GetErrorCode(long result) {
		return Zeze.Net.Protocol.getProtocolId(result);
	}
}
