package Zeze.Web;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Web.BHeader;
import Zeze.Builtin.Web.BRequest;
import Zeze.Builtin.Web.BResponse;
import Zeze.Builtin.Web.BStream;
import Zeze.Net.Binary;
import com.sun.net.httpserver.HttpExchange;

public class LinkdHttpExchange {
	final long exchangeId = 0; // todo generate
	final HttpExchange exchange;
	public final int FlagInputClosed = 1;
	public final int FlagOutputClosed = 2;
	public final int FlagStreamClosed = FlagInputClosed + FlagOutputClosed;

	private int Flags = 0;

	public boolean isInputStreamClosed() {
		return (FlagStreamClosed & FlagInputClosed) != 0;
	}

	public boolean isOutputStreamClosed() {
		return (FlagStreamClosed & FlagOutputClosed) != 0;
	}

	public void closeOutputStream() {
		setFlagsAndTryClose(FlagOutputClosed);
	}

	public LinkdHttpExchange(HttpExchange x) {
		exchange = x;
	}

	public void close() {
		exchange.close();
		// todo LinkdHttpExchangeManager
	}

	private void setFlagsAndTryClose(int flag) {
		Flags += flag;
		if ((Flags & FlagStreamClosed) == FlagStreamClosed)
			close();
	}

	public void fillInput(BStream s) throws IOException {
		s.setExchangeId(exchangeId);
		var body = tryReadInputBody();
		if (null != body)
			s.setBody(body);
		s.setFinish(null == body);
	}

	/**
	 * 注意填充req时不会重置其中的变量，所以要求req必须是新创建的Rpc的Argument。
	 * 不能重用Rpc和BRequest。
	 * @param req
	 * @throws IOException
	 */
	public void fillRequest(BRequest req) throws IOException {
		req.setMethod(exchange.getRequestMethod());
		req.setPath(exchange.getRequestURI().getPath());
		req.setQuery(exchange.getRequestURI().getQuery());
		for (var e : exchange.getRequestHeaders().entrySet()) {
			var header = new BHeader();
			header.getValues().addAll(e.getValue());
			req.getHeaders().put(e.getKey(), header);
		}

		var body = tryReadInputBody();
		if (null != body)
			req.setBody(body);
		req.setRemainInputStream(null == body);
	}

	private Binary tryReadInputBody() throws IOException {
		// 本质上linkd跟异步的HttpServer才是最匹配的。
		// 由于Jdk自带HttpServer是同步的，下面的写法尝试最小化阻塞时间。
		// 但不能避免阻塞，考虑使用【zp的支持异步的HttpServer】。
		var inputStream= exchange.getRequestBody();
		var available = inputStream.available();
		if (available > 8192)
			available = 8192; // 跟Server之间是包转发，最多一次读取这么多。
		else if (available < 1024)
			available = 1024; // 数据没有准备好，最少读取这个多，这个数字参考了ip包大小（链路层数据包）。

		var body = new byte[available];
		var size = inputStream.read(body);
		if (size >= 0)
			return new Binary(body, 0, size);

		inputStream.close();
		setFlagsAndTryClose(FlagInputClosed);
		return null;
	}

	public void sendResponse(BResponse response) throws IOException {
		var headers = exchange.getResponseHeaders();
		for (var e : response.getHeaders()) {
			var list = new ArrayList(e.getValue().getValues().size());
			list.addAll(e.getValue().getValues());
			headers.put(e.getKey(), list);
		}

		var bytes = response.getBody().InternalGetBytesUnsafe();
		exchange.sendResponseHeaders(response.getCode(), 0);
		var body = exchange.getResponseBody();
		body.write(bytes);

		if (response.isFinish()) {
			body.close();
			setFlagsAndTryClose(FlagOutputClosed);
		}
	}

	public void sendErrorResponse(String message) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=utf-8"));

		var bytes = message.getBytes(StandardCharsets.UTF_8);
		var len = bytes.length;
		exchange.sendResponseHeaders(200, len > 0 ? len : -1);
		if (len > 0) {
			try (var body = exchange.getResponseBody()) {
				body.write(bytes);
			} finally {
				setFlagsAndTryClose(FlagOutputClosed);
			}
		}
	}

	public void sendErrorResponse(Throwable ex) throws IOException {
		exchange.getResponseHeaders().put("Content-Type", List.of("text/plain; charset=UTF-8"));
		exchange.sendResponseHeaders(200, 0);
		try (var body = exchange.getResponseBody()) {
			ex.printStackTrace(new PrintStream(body, false, StandardCharsets.UTF_8));
		} finally {
			setFlagsAndTryClose(FlagOutputClosed);
		}
	}
}
