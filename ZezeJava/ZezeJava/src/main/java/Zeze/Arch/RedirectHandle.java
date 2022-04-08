package Zeze.Arch;

import Zeze.Net.Binary;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		Binary call(long sessionId, int hash, Binary encodedParams) throws Throwable;
	}

	public final TransactionLevel RequestTransactionLevel;
	public final IRequestHandle RequestHandle;

	public RedirectHandle(IRequestHandle requestHandle) {
		RequestTransactionLevel = TransactionLevel.Serializable;
		RequestHandle = requestHandle;
	}

	public RedirectHandle(TransactionLevel requestTransactionLevel, IRequestHandle requestHandle) {
		RequestTransactionLevel = requestTransactionLevel;
		RequestHandle = requestHandle;
	}
}
