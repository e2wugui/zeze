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

	public final int ReturnCode(short code) {
		int c = code >= 0 ? code : ((int)code) & 0xffff;
		return getId() << 16 | c;
	}
}