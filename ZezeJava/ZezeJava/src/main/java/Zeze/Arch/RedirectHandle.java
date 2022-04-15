package Zeze.Arch;

import java.util.function.Function;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;

public class RedirectHandle {
	public interface IRequestHandle {
		/**
		 * @param sessionId     只用于All模式
		 * @param hash          serverId或hash
		 * @param encodedParams 输入参数的序列化
		 * @param asyncContext  只用于All模式
		 * @return Binary(用于All模式输出结果的序列化, ASYNC_RESULT表示异步); RedirectFuture(用于非All模式); null(用于非All模式没有返回值)
		 */
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
