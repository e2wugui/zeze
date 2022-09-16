package Zeze.Web;

/*
 * HttpExchange 我们自己的包装和Jdk.HttpServer里面同名。下面用Linkd.HttpExchange表示Jdk的对象。
 * Server.HttpExchange 生命期管理:
 * 1. 正常请求结束，sendResponseHeaders with finish == true. auto call closeResponseBody()。
 * 2. ResponseOutputStream 结束，sendResponseBody or sendResponseBodyAsync with finish == true. auto call closeResponseBody().
 * 3. state==Requesting时发生异常，把 Stacktrace 发送给linkd并在浏览器中显示。
 * 4. state==Streaming时发生异常，close(Procedure.Exception)
 * 5. 处理函数（目前只有sendResponseBodyAsync的回调）返回非0值，close(errorCode)
 * 6. close with error != 0 || null != ex 时发送CloseExchange给linkd。
 *
 * Linkd.HttpExchange 生命期管理：
 * 1. 正常请求结束，收到Request.Result并且finish == true. auto call closeResponseBody();
 * 2. InputStream 读取完毕，设置 closeRequestBody();
 * 3. ResponseOutputStream with finish == true; closeResponseBody();
 * 4. IdleTimeout: close(Procedure.Timeout);
 * 5. close with error != 0 || null != ex 时发送CloseExchange给server。
 *
 * 上传文件流
 * linkd 收到 Request.Result 后，如果InputStream还有数据，将主动读取并打包到InputStreamRequestInputStream，发送给server。
 * server处理上传文件流总是异步的。在HttpServlet里面实现OnUpload处理上传流。
 *
 * 多线程安全
 * HttpExchange本身不是线程安全的，不要在多个线程中共享。
 */

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import Zeze.Builtin.Web.BHeader;
import Zeze.Builtin.Web.BRequest;
import Zeze.Builtin.Web.CloseExchange;
import Zeze.Builtin.Web.Request;
import Zeze.Builtin.Web.ResponseOutputStream;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.Str;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpExchange {
	static final Logger logger = LogManager.getLogger(HttpExchange.class);
	private static final byte REQUESTING = 0;
	private static final byte RESPONSE_HEADERS_SENT = 1;

	private final Web web;
	private final Request request;
	private byte state;
	private boolean requestBodyClosed;
	private boolean responseBodyClosed;

	public HttpExchange(Web web, Request r) {
		this.web = web;
		request = r;
	}

	public final Web getWeb() {
		return web;
	}

	public final String getRequestMethod() {
		return request.Argument.getMethod();
	}

	public final BRequest getRequest() {
		return request.Argument;
	}

	void closeRequestBody() {
		if (requestBodyClosed)
			return;
		requestBodyClosed = true;
		tryClose();
	}

	private void closeResponseBody() {
		if (responseBodyClosed)
			return;
		responseBodyClosed = true;
		tryClose();
	}

	private void tryClose() {
		//logger.info("tryClose " + requestBodyClosed + " " + responseBodyClosed, new RuntimeException());
		if (requestBodyClosed && responseBodyClosed)
			close(0, null, null, false);
	}

	long close(long error, String msg, Throwable ex, boolean notifyLinkd) {
		if (null == web.exchanges(request).remove(request.Argument.getExchangeId()))
			return error;

		logger.debug("close: " + error + " " + msg + request.Argument.getPath(), ex);
		if (error != 0 || null != ex)
			logger.error(msg, ex);

		if (state == REQUESTING) {
			// 请求处理过程中的错误通过Rpc.Result报告。
			if (null != msg)
				request.Result.setMessage(msg);
			if (null != ex)
				request.Result.setStacktrace(Str.stacktrace(ex));
			request.Result.setFinish(true);
			request.trySendResultCode(error);
		} else if (notifyLinkd) {
			// 其他过程(现在只有sendResponseBody)通过专门的Rpc报告错误。
			var ce = new CloseExchange();
			ce.Argument.setExchangeId(request.Argument.getExchangeId());
			ce.Send(request.getSender()); // no wait
		}
		return 0;
	}

	public Map<String, BHeader> getRequestHeaders() {
		// 这个方法不检查State，对于不正确的状态，得到以后即使修改将不会有太坏影响。
		return request.Argument.getHeaders();
	}

	public Map<String, BHeader> getResponseHeaders() {
		// 这个方法不检查State，对于不正确的状态，得到以后即使修改将不会有太坏影响。
		return request.Result.getHeaders();
	}

	public List<String> getRequestCookie() {
		return getRequestHeader("Cookie");
	}

	public List<String> getRequestHeader(String key) {
		var header = getResponseHeaders().get(key);
		if (null == header)
			return null;
		return header.getValues();
	}

	public void setResponseCookie(String... values) {
		setResponseHeader("Set-Cookie", values);
	}

	public void setResponseHeader(String key, List<String> values) {
		if (null != values) {
			var header = new BHeader();
			for (var v : values)
				header.getValues().add(v);
			getResponseHeaders().put(key, header);
		}
	}

	public void setResponseHeader(String key, String... values) {
		var header = new BHeader();
		for (var v : values)
			header.getValues().add(v);
		getResponseHeaders().put(key, header);
	}

	/**
	 * @param code http result code
	 * @param body maybe null
	 */
	public void sendResponseHeaders(int code, byte[] body, boolean finish) {
		if (state != REQUESTING)
			throw new IllegalStateException("Not In State.Requesting.");

		request.Result.setCode(code);
		if (null != body)
			request.Result.setBody(new Binary(body));
		request.Result.setFinish(finish);
		request.SendResult();
		state = RESPONSE_HEADERS_SENT;
		if (finish)
			closeResponseBody();
	}

	public void sendResponseBody(byte[] body, boolean finish) {
		if (state != RESPONSE_HEADERS_SENT)
			throw new IllegalStateException("Not In State.ResponseHeadersSent.");

		final var stream = new ResponseOutputStream();
		stream.Argument.setExchangeId(request.Argument.getExchangeId());
		stream.Argument.setBody(new Binary(body));
		stream.Argument.setFinish(finish);
		stream.SendForWait(request.getSender()).await();
		if (stream.getResultCode() != 0)
			throw new IllegalStateException("Stream.ResultCode=" + stream.getResultCode());
		if (finish)
			closeResponseBody();
	}

	interface ISendDone {
		long call(HttpExchange he, long resultCode);
	}

	public void sendResponseBodyAsync(byte[] body, boolean finish, ISendDone sendDone) {
		if (state != RESPONSE_HEADERS_SENT)
			throw new IllegalStateException("Not In State.ResponseHeadersSent.");

		final var stream = new ResponseOutputStream();
		stream.Argument.setExchangeId(request.Argument.getExchangeId());
		stream.Argument.setBody(new Binary(body));
		stream.Argument.setFinish(finish);
		stream.Send(request.getSender(), (p) -> {
			try {
				var rc = sendDone.call(this, stream.getResultCode());
				if (rc != 0)
					close(rc, "sendDone.callback return error.", null, true);
				return rc;
			} catch (Throwable ex) {
				close(Procedure.Exception, "sendDone.callback throw exception.", ex, true);
				return Procedure.Exception;
			}
		});
		if (finish)
			closeResponseBody();
	}

	public void sendTextResponse(String text) {
		setResponseHeader("Content-Type", "text/plain; charset=utf-8");
		sendResponseHeaders(200, text.getBytes(StandardCharsets.UTF_8), true);
	}
}
