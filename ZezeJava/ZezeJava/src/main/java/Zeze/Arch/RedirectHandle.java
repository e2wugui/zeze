package Zeze.Arch;

import java.util.function.Function;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		/**
		 * @param hash          serverId或hash
		 * @param encodedParams 输入参数的序列化
		 * @return RedirectFuture(用于非All模式); RedirectAllFuture(用于All模式); null(用于非All模式没有返回值)
		 */
		Object call(int hash, Binary encodedParams) throws Throwable;
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
