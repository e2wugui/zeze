package Zeze;

public abstract class IModule {
	private String FullName;
	public String getFullName() {
		return FullName;
	}
	private String Name;
	public String getName() {
		return Name;
	}
	private int Id;
	public int getId() {
		return Id;
	}

	public void UnRegister() { // 为了重新装载 Module 的补丁。注册在构造函数里面进行。
	}

	public final int ReturnCode(int code) {
		if (code < 0 || code > Short.MAX_VALUE)
			throw new RuntimeException("return code too big");
		return ReturnCode((short)code);
	}

	public final int ReturnCode(short code) {
		return getId() << 16 | (code & 0xffff);
	}
}