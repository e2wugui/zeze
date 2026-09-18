package Zeze.Net;

public class FamilyClass {
	public static final int Protocol = 2;
	public static final int Request = 1;
	public static final int Response = 0;
	public static final int RaftRequest = 4;
	public static final int RaftResponse = 3;

	public static final int BitResultCode = 1 << 5;
	public static final int FamilyClassMask = BitResultCode - 1;

	public static boolean isRpc(int familyClass) {
		return familyClass <= Request;
	}

	public static boolean isRaftRpc(int familyClass) {
		// 枚举相等判定（FND8-54）：与isRpc的严格性对齐——宽松的>=会把非法familyClass(5..31)
		// 当合法Raft应答接受，RaftRpc.decode的setRequest恒false按Response静误解，
		// 排障信号从invalid-header退化为lost-context/bean解码错。
		return familyClass == RaftResponse || familyClass == RaftRequest;
	}
}
