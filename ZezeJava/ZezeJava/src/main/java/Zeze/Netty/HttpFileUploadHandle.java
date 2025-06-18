package Zeze.Netty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.MemoryFileUpload;
import org.jetbrains.annotations.NotNull;

public class HttpFileUploadHandle implements HttpMultipartHandle {

	public static boolean isMultipartRequest(@NotNull HttpRequest request) {
		// 1. 获取 Content-Type 请求头
		String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
		// 2. 判断是否为 multipart
		return contentType != null && contentType.toLowerCase().startsWith("multipart/");
	}

	public static int MemoryBufSize = 16 * 1024;
	private boolean multipart;
	private ByteBuf memoryBuf;
	private File fileUploadTmp;
	private OutputStream fileUploadOs;
	private String filename; // 用户指定的文件名。通过url得到。

	@Override
	public void onBeginStream(@NotNull HttpExchange x, long from, long to, long size) throws Exception {
		multipart = isMultipartRequest(Objects.requireNonNull(x.request()));
		if (multipart) {
			HttpMultipartHandle.super.onBeginStream(x, from, to, size);
		} else {
			memoryBuf = Unpooled.buffer(); // 首先缓存，超过16k后使用文件。后面判断。
			filename = x.queryMap().get("filename");
			if (null == filename)
				filename = "upload";
		}
	}

	@Override
	public void onStreamContent(@NotNull HttpExchange x, @NotNull HttpContent content) throws Exception {
		if (multipart) {
			HttpMultipartHandle.super.onStreamContent(x, content);
		} else if (fileUploadOs != null) {
			// 这里使用array等安全吗？
			var buf = content.content();
			fileUploadOs.write(buf.array(), buf.arrayOffset(), buf.readableBytes());
		} else {
			var buf = content.content();
			memoryBuf.writeBytes(buf);
			if (memoryBuf.readableBytes() >= MemoryBufSize) {
				fileUploadTmp = File.createTempFile("ZezeHttpUpload", ".tmp");
				fileUploadOs = new FileOutputStream(fileUploadTmp);
				// memoryBuf 直接是堆内存，这里使用array没问题了吧。
				fileUploadOs.write(memoryBuf.array(), memoryBuf.arrayOffset(), memoryBuf.readableBytes());
				memoryBuf = null; // 已经写入文件，并且fileUploadOs已经创建，可以release了。
			}
		}
	}

	@Override
	public void onEndStream(@NotNull HttpExchange x) throws Exception {
		if (multipart)
			HttpMultipartHandle.super.onEndStream(x);
		else {
			if (null != fileUploadOs)
				fileUploadOs.close();

			if (null != memoryBuf){
				var fileUpload = new MemoryFileUpload("ZezeHttpUpload", filename,
						"application/octet-stream", "utf-8",
						null, memoryBuf.readableBytes());
				fileUpload.setContent(memoryBuf);
				onFileCompleted(x, fileUpload);
			} else if (fileUploadTmp != null) {
				// 这里第一个参数name是multipart模式中的name，在这里固定命名。
				var fileUpload = new DiskFileUpload("ZezeHttpUpload", filename,
						"application/octet-stream", "utf-8",
						null, fileUploadTmp.length());
				fileUpload.setContent(fileUploadTmp);
				onFileCompleted(x, fileUpload);
			} else {
				throw new IllegalStateException();
			}
		}
	}
}