package Zeze.Netty;

import java.io.File;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AsciiString;
import org.jspecify.annotations.Nullable;

/**
 * 静态文件服务实现（供 HttpExchange.sendFile/sendPath 委托）：条件请求、Range、零拷贝与目录列表。
 */
final class HttpFileService {
	private HttpFileService() {
	}

	private static final Pattern rangePattern = Pattern.compile("[ =\\-/]");
	private static final OpenOption[] readOnlyOpenOptions = new OpenOption[]{StandardOpenOption.READ};
	private static final int FileChunkSize = 64 * 1024;

	// 剥离 CR/LF 并转义引号，防止文件名在未开启 Netty 头校验时注入响应头。
	private static String sanitizeFilenameForHeader(String fn) {
		var sb = new StringBuilder(fn.length() + 16);
		for (int i = 0, n = fn.length(); i < n; ++i) {
			var c = fn.charAt(i);
			if (c == '\r' || c == '\n')
				continue;
			if (c == '"')
				sb.append("\\\"");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	static void sendFile(HttpExchange x, File file, int fileCacheSeconds) throws Exception {
		HttpExchange.requireNonTransactionalStreaming();
		if (!file.isFile()) {
			x.close(x.send404());
			return;
		}
		var req = x.request;
		if (req == null) {
			x.close(x.send500(""));
			return;
		}

		// 检查 if-modified-since
		var lastModified = file.lastModified() / 1000;
		var ifModifiedSince = req.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty() && lastModified == HttpServer.parseDate(ifModifiedSince)) {
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED,
				Unpooled.EMPTY_BUFFER, HttpExchange.headersFactory, HttpExchange.trailersFactory);
			HttpServer.setDate(res.headers())
				.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			if (!x.isH2())
				res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			x.close(x.writeResponse(res, true, null));
			return;
		}

		var fn = file.getName();
		var fc = FileChannel.open(file.toPath(), readOnlyOpenOptions);
		try {
			var fsize = fc.size();
			var rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
			// 多段 Range 暂不支持，降级为 200 全量发送。
			var r = rangeHeader != null && rangeHeader.indexOf(',') < 0 ? parseRange(req, HttpHeaderNames.RANGE) : null;

			var from = 0L;
			var to = fsize - 1L;
			var partial = false;
			if (r != null && (r[0] >= 0 || r[1] >= 0)) {
				var satisfiable = false;
				if (r[0] >= 0) { // bytes=from-[to]
					satisfiable = r[0] < fsize && (r[1] < 0 || r[1] >= r[0]);
					if (satisfiable) {
						from = r[0];
						if (r[1] >= 0)
							to = Math.min(r[1], fsize - 1);
					}
				} else { // 后缀形式 bytes=-N
					satisfiable = r[1] > 0 && fsize > 0;
					if (satisfiable)
						from = Math.max(fsize - r[1], 0);
				}
				if (!satisfiable) {
					var res416 = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE, Unpooled.EMPTY_BUFFER,
						HttpExchange.headersFactory, HttpExchange.trailersFactory);
					HttpServer.setDate(res416.headers())
						.set(HttpHeaderNames.CONTENT_LENGTH, 0)
						.set(HttpHeaderNames.CONTENT_RANGE, "bytes */" + fsize);
					if (!x.isH2())
						res416.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
					fc.close();
					x.close(x.writeResponse(res416, true, null));
					return;
				}
				partial = true;
			}
			var contentLen = partial ? to - from + 1 : fsize;

			var res = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				partial ? HttpResponseStatus.PARTIAL_CONTENT : HttpResponseStatus.OK, HttpExchange.headersFactory);
			var headers = HttpServer.setDate(res.headers())
				.set(HttpHeaderNames.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeFilenameForHeader(fn) + '"')
				.set(HttpHeaderNames.CONTENT_TYPE, Mimes.fromFileName(fn))
				.set(HttpHeaderNames.CONTENT_LENGTH, contentLen)
				.set(HttpHeaderNames.EXPIRES, HttpServer.getDate(HttpServer.getLastDateSecond() + fileCacheSeconds))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + fileCacheSeconds)
				.set(HttpHeaderNames.LAST_MODIFIED, HttpServer.getDate(lastModified));
			if (!x.isH2())
				headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			if (partial)
				headers.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + '-' + to + '/' + fsize);
			x.writeResponse(res, false, x.context.voidPromise());

			ChannelFuture lastFuture;
			if (contentLen > 0 && !HttpMethod.HEAD.equals(req.method())) {
				if (x.isH2()) {
					lastFuture = H2FileSender.send(x, fc, from, contentLen, FileChunkSize);
				} else if (x.context.pipeline().<@Nullable SslHandler>get(SslHandler.class) != null) {
					// SSL 只接收 ByteBuf；HttpChunkedInput 自带终结帧，无需另写 EMPTY_LAST_CONTENT。
					lastFuture = x.writeResponse(new HttpChunkedInput(new ChunkedNioFile(fc, from, contentLen,
							FileChunkSize)), true, null);
				} else {
					// 零拷贝发送文件，完成后经 listener 关闭 FileChannel。
					x.writeResponse(new DefaultFileRegion(fc, from, contentLen), false, x.context.voidPromise());
					lastFuture = x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
				}
			} else
				lastFuture = x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
			lastFuture.addListener(__ -> fc.close());
			x.close(lastFuture);
		} catch (Throwable e) {
			fc.close(); // 兜底关闭，防 listener 挂接前异常泄漏 FD
			throw e;
		}
	}

	static void sendPath(HttpExchange x, File file) {
		HttpExchange.requireNonTransactionalStreaming();
		if (!file.isDirectory() || file.isHidden()) {
			x.close(x.send404());
			return;
		}

		int fileLimit = 10000; // 限制最多列出多少目录+文件,避免开销太大
		var fn = htmlEscape(file.getName());
		var sb = new StringBuilder("<html><head><title>Index of ").append(fn)
			.append("/</title></head><body><h1>Index of ").append(fn)
			.append("/</h1><hr><pre><a href=\"../\">../</a>\n");
		var fs = file.listFiles();
		if (fs != null) {
			for (var f : fs) {
				if (f.isDirectory() && !f.isHidden()) { // 先列目录
					if (--fileLimit < 0) {
						sb.append("......\n");
						break;
					}
					fn = htmlEscape(f.getName());
					sb.append(String.format("%s %18s <a href=\"%s/\">%s/</a>\n",
						listTime(f.lastModified()), "",
						fileLink(f.getName()), fn));
				}
			}
			for (var f : fs) {
				if (!f.isDirectory() && !f.isHidden()) { // 再列文件
					if (--fileLimit < 0) {
						sb.append("......\n");
						break;
					}
					fn = htmlEscape(f.getName());
					sb.append(String.format("%s %,18d <a href=\"%s\">%s</a>\n",
						listTime(f.lastModified()), f.length(),
						fileLink(f.getName()), fn));
				}
			}
		}
		x.close(x.sendHtml(HttpResponseStatus.OK, sb.append("</pre><hr></body></html>").toString()));
	}

	private static String fileLink(String name) {
		// href 是单个 URL 路径段；显示文本仍单独做 HTML 转义。
		return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private static final DateTimeFormatter listTimeFormat =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static String listTime(long epochMilli) {
		return listTimeFormat.format(Instant.ofEpochMilli(epochMilli));
	}

	// 下载请求/上传回复: range: bytes=[from]-[to]，闭区间[from,to]；bytes=-N为最后N字节
	// 上传请求/下载回复: content-range: bytes from-to/size 范围是[from,to]
	// 参考: https://www.jianshu.com/p/acca9656e250
	// 返回: [from, to, size]
	static long [] parseRange(HttpRequest req, AsciiString headerName) {
		var r = new long[]{-1, -1, -1};
		var headers = req.headers();
		var range = headers.get(headerName);
		if (range != null) {
			var p = range.indexOf(',');
			if (p >= 0)
				range = range.substring(0, p); // 暂不支持",",只下载第1段
			var ss = rangePattern.split(range);
			var sn = ss.length;
			if (sn > 1) {
				r[0] = parse(ss[1]);
				if (sn > 2) {
					r[1] = parse(ss[2]);
					if (sn > 3)
						r[2] = parse(ss[3]);
				}
			}
		} else if (headerName == HttpHeaderNames.CONTENT_RANGE) { // 如果没找到content-range可能只用content-length大小上传
			var len = headers.get(HttpHeaderNames.CONTENT_LENGTH);
			if (len != null) {
				var n = parse(len);
				if (n >= 0) {
					r[0] = 0;
					r[1] = n - 1;
					r[2] = n;
				}
			}
		}
		return r;
	}

	static long parse(String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return -1;
		}
	}

	private static String htmlEscape(String s) {
		var sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case '<' -> sb.append("&lt;");
			case '>' -> sb.append("&gt;");
			case '&' -> sb.append("&amp;");
			case '"' -> sb.append("&quot;");
			case '\'' -> sb.append("&#39;");
			default -> sb.append(s.charAt(i));
			}
		}
		return sb.toString();
	}
}
