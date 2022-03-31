package Zeze.Arch;

import Zeze.Net.Binary;

public class Return {
	public long ReturnCode;
	public Binary EncodedParameters;
	public Return(long rc, Binary params) {
		ReturnCode = rc;
		EncodedParameters = params;
	}
}
