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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public int ReturnCode(ushort code)
	public final int ReturnCode(short code) {
		return getId() << 16 | code;
	}
}