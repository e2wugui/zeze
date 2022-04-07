package Zeze;

public abstract class IModule {
	public abstract String getFullName();

	public abstract String getName();

	public abstract int getId();

	public void UnRegister() { // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
	}

	public final long ErrorCode(int code) {
		if (code < 0)
			throw new IllegalArgumentException("code must greater than 0.");
		return Zeze.Net.Protocol.MakeTypeId(getId(), code);
	}

	public void Initialize(Zeze.AppBase app) {

	}

	public static int GetModuleId(long result) {
		return Zeze.Net.Protocol.GetModuleId(result);
	}

	public static int GetErrorCode(long result) {
		return Zeze.Net.Protocol.GetProtocolId(result);
	}
}
