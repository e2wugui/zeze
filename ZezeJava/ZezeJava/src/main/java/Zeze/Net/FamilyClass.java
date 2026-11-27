package Zeze.Net;

/**
 * 协议家族类别常量（协议帧头低5位）与判定。
 */
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
		// 严格枚举相等，避免非法值（5..31）被误判为 Raft 协议。
		return familyClass == RaftResponse || familyClass == RaftRequest;
	}
}
