package Zeze.Arch;

import java.util.function.Function;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		Object call(long sessionId, int hash, Binary encodedParams, Object asyncContext) throws Throwable;
	}

	public static final Binary ASYNC_RESULT = new Binary(ByteBuffer.Empty);

	public final TransactionLevel RequestTransactionLevel;
	public final IRequestHandle RequestHandle;
	public final Function<Object, Binary> ResultEncoder;

	public RedirectHandle(TransactionLevel requestTransactionLevel,
						  IRequestHandle requestHandle,
						  Function<Object, Binary> resultEncoder) {
		RequestTransactionLevel = requestTransactionLevel;
		RequestHandle = requestHandle;
		ResultEncoder = resultEncoder;
	}
}
