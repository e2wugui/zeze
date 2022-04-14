package Zeze.Arch;

import java.util.function.Function;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		Binary call(long sessionId, int hash, Binary encodedParams, Object asyncContext) throws Throwable;
	}

	public static final Binary ASYNC_RESULT = new Binary(ByteBuffer.Empty);

	public final TransactionLevel RequestTransactionLevel;
	public final Function<RedirectResult, Binary> ResultEncoder;
	public final IRequestHandle RequestHandle;

	public RedirectHandle(TransactionLevel requestTransactionLevel,
						  Function<RedirectResult, Binary> resultEncoder,
						  IRequestHandle requestHandle) {
		RequestTransactionLevel = requestTransactionLevel;
		RequestHandle = requestHandle;
		ResultEncoder = resultEncoder;
	}
}
