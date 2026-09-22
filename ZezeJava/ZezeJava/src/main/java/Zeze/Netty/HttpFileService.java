package Zeze.Netty;

import java.io.File;
import java.nio.channels.FileChannel;
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
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AsciiString;
import org.jetbrains.annotations.NotNull;

// HttpExchange静态文件服务的实现体（sendFile/sendPath的委托目标，从HttpExchange拆出）：
// 条件请求（If-Modified-Since/Range）、缓存头、零拷贝FileRegion、目录列表。仅包内使用。
final class HttpFileService {
	private HttpFileService() {
	}

	private static final @NotNull Pattern rangePattern = Pattern.compile("[ =\\-/]");
	private static final OpenOption[] readOnlyOpenOptions = new OpenOption[]{StandardOpenOption.READ};
	// h2分块发送的块大小：DATA帧逐块转发（ChunkedWriteHandler拉取），64KB平衡帧数与内存驻留
	private static final int H2FileChunkSize = 64 * 1024;

	// 文件名来自磁盘（Linux允许CR/LF/引号入名），且headersFactory关闭了Netty头校验：CRLF直拼构成响应头注入，引号破坏quoted-string边界。
	private static String sanitizeFilenameForHeader(@NotNull String fn) {
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

	static void sendFile(@NotNull HttpExchange x, @NotNull File file, int fileCacheSeconds) throws Exception {
		var req = x.request;
		if (req == null) {
			x.close(x.send500(""));
			return;
		}

		// 检查 if-modified-since
		var lastModified = file.lastModified() / 1000;
		var ifModifiedSince = req.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.isEmpty() && lastModified == HttpServer.parseDate(ifModifiedSince)) {
			var res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, // 文件未改变
				Unpooled.EMPTY_BUFFER, HttpExchange.headersFactory, HttpExchange.trailersFactory);
			HttpServer.setDate(res.headers())
				.set(HttpHeaderNames.CONTENT_LENGTH, 0);
			if (!x.isH2()) // h2禁连接管理头
				res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			x.close(x.writeResponse(res, true, null)); // 经序化器：保活响应直写会在pipelining下先于前序响应上线
			return;
		}

		var fn = file.getName();
		var fc = FileChannel.open(file.toPath(), readOnlyOpenOptions);
		try {
			// open成功到lastFuture的listener接管fc关闭之间（如fc.size()抛IO异常），fc由catch兜底关闭，
			// 否则异常传播回handler后文件句柄泄漏。fc.close()幂等，与listener互斥安全。
			var fsize = fc.size();
			var rangeHeader = req.headers().get(HttpHeaderNames.RANGE);
			// RFC 7233: 多段Range需multipart/byteranges（暂不支持），降级为200全量（服务器可忽略Range）
			var r = rangeHeader != null && rangeHeader.indexOf(',') < 0 ? parseRange(req, HttpHeaderNames.RANGE) : null;

			var from = 0L;
			var to = fsize - 1L;
			var partial = false;
			if (r != null && (r[0] >= 0 || r[1] >= 0)) { // 语法有效的单段Range
				var satisfiable = false;
				if (r[0] >= 0) { // bytes=from-[to]；可满足: from在文件内且(to缺失或to>=from)
					satisfiable = r[0] < fsize && (r[1] < 0 || r[1] >= r[0]);
					if (satisfiable) {
						from = r[0];
						if (r[1] >= 0)
							to = Math.min(r[1], fsize - 1); // to是inclusive右端点，超出文件尾按fsize-1截断
					}
				} else { // 后缀形式 bytes=-N；可满足: N>0且文件非空（bytes=-0按RFC不可满足）
					satisfiable = r[1] > 0 && fsize > 0;
					if (satisfiable)
						from = Math.max(fsize - r[1], 0);
				}
				if (!satisfiable) { // RFC要求416 + Content-Range: bytes */fsize（客户端据此检测远端文件截断）
					var res416 = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE, Unpooled.EMPTY_BUFFER,
						HttpExchange.headersFactory, HttpExchange.trailersFactory);
					HttpServer.setDate(res416.headers())
						.set(HttpHeaderNames.CONTENT_LENGTH, 0)
						.set(HttpHeaderNames.CONTENT_RANGE, "bytes */" + fsize);
					if (!x.isH2()) // h2禁连接管理头
						res416.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
					fc.close();
					x.close(x.writeResponse(res416, true, null)); // 经序化器，同304分支
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
			if (!x.isH2()) // h2禁连接管理头
				headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			if (partial) // Content-Range只属于206/416，200不带
				headers.set(HttpHeaderNames.CONTENT_RANGE, "bytes " + from + '-' + to + '/' + fsize);
			x.writeResponse(res, false, x.context.voidPromise()); // N①：响应头经序化器（挂起时FileRegion同队保序）

			ChannelFuture lastFuture;
			if (contentLen > 0 && !HttpMethod.HEAD.equals(req.method())) {
				if (x.isH2()) {
					// h2无FileRegion零拷贝：分块读文件为DefaultHttpContent逐块经writeResponse（帧翻译codec转
					// DATA帧）。同步读循环跑在dispatch任务线程：本地文件读毫秒级，出站仅入netty发送队列不阻塞
					// socket。慢消费者的内存界后续按可写性分块收紧（对齐ChunkedWriteHandler语义）——
					// ChunkedWriteHandler+HttpChunkedInput组合在h2子channel上排水不触发（写future永驻），
					// 弃用。零拷贝损失为h2帧化的固有代价。LastHttpContent收尾=endStream。
					var nioBuf = java.nio.ByteBuffer.allocate(H2FileChunkSize);
					long pos = from;
					long remain = contentLen;
					while (remain > 0) {
						nioBuf.clear();
						nioBuf.limit((int)Math.min(nioBuf.capacity(), remain));
						int n = fc.read(nioBuf, pos);
						if (n < 0)
							break; // 文件被截断：头部CL失配，客户端按帧序可诊断
						if (n > 0) {
							// copiedBuffer必须复制：wrappedBuffer引用的array会被下一轮read覆盖。
							// 每块即flush：跨块汇流依赖flush边界（迟滞冲刷在高水位下可能卡住出站）。
							x.writeResponse(new DefaultHttpContent(io.netty.buffer.Unpooled.copiedBuffer(
									nioBuf.array(), 0, n)), true, x.context.voidPromise());
							pos += n;
							remain -= n;
						}
					}
					lastFuture = x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
				} else {
					// 发文件任务全部交给Netty（零拷贝），并且发送完毕时关闭。
					x.writeResponse(new DefaultFileRegion(fc, from, contentLen), false, x.context.voidPromise());
					lastFuture = x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
				}
			} else
				lastFuture = x.writeResponse(LastHttpContent.EMPTY_LAST_CONTENT, true, null);
			lastFuture.addListener(__ -> fc.close());
			x.close(lastFuture);
		} catch (Throwable e) {
			fc.close(); // 兜底：listener接管前的异常窗口；幂等，listener已关则无效果
			throw e;
		}
	}

	static void sendPath(@NotNull HttpExchange x, @NotNull File file) {
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
						listTime(f.lastModified()), "", fn, fn));
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
						listTime(f.lastModified()), f.length(), fn, fn));
				}
			}
		}
		x.close(x.sendHtml(HttpResponseStatus.OK, sb.append("</pre><hr></body></html>").toString()));
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
	static long @NotNull [] parseRange(@NotNull HttpRequest req, @NotNull AsciiString headerName) {
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
				var n = parse(len); // 复用容错解析：畸形或超int范围的值按无range处理
				if (n >= 0) {
					r[0] = 0;
					r[1] = n - 1;
					r[2] = n;
				}
			}
		}
		return r;
	}

	static long parse(@NotNull String s) {
		if (s.isEmpty())
			return -1;
		try {
			return Long.parseLong(s);
		} catch (Exception ignored) {
			return -1;
		}
	}

	// sendPath的目录列表把文件名/目录名直接拼进HTML的title/h1与<a href>属性：
	// 含"、<、>、&等字符的文件名（目录内容或请求路径可被控制）可注入脚本。统一转义。
	private static @NotNull String htmlEscape(@NotNull String s) {
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
