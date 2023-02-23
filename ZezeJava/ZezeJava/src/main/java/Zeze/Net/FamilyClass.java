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
		return familyClass >= RaftResponse;
	}
}
