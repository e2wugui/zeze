package Zeze.Services;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.zip.ZipFile;
import Zeze.AppBase;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpFileUploadHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ClassReloader;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 【安全警示】任意字节码注入端点：上传的zip内class被ClassReloader直接替换进已加载类
 * （热更新），触达且持有token者即获得任意代码执行权（替换任意类），仍必须仅绑定回环/
 * 内网，绝不可暴露公网。token校验（FND8-66）把"部署纪律"升级为代码不变量：构造时
 * 显式传入token，或传null由启动期自动生成随机token并打日志（零配置可用，运维从日志
 * 取token）；请求须以X-Zeze-Token头或"token"查询参数携带，常量时间比较，失配答403。
 */
public class ReloadClassServer implements HttpFileUploadHandle {
	private static final @NotNull org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(ReloadClassServer.class);
	static final String TOKEN_HEADER = "X-Zeze-Token";
	static final String TOKEN_QUERY_KEY = "token";
	private final String uploadDir;
	private final String fileVarName;
	private final String token;

	/**
	 * 构造ReloadClassServer（token自动生成，见类注释）。
	 *
	 * @param app         应用App实例
	 * @param urlPath     上传文件的urlPath
	 * @param uploadDir   上传文件的目录
	 * @param fileVarName 上传文件名查询Key，
	 *                    比如直接上传的url:/upload?fileVarName=xxx
	 *                    或者表单中&lt;input type="file" name="fileVarName" multiple&gt;
	 */
	public ReloadClassServer(@NotNull AppBase app,
							 @NotNull String urlPath,
							 @NotNull String uploadDir,
							 @NotNull String fileVarName) {
		this(app, urlPath, uploadDir, fileVarName, null);
	}

	/**
	 * 构造ReloadClassServer。
	 *
	 * @param token 鉴权token；null/空白时自动生成随机token并打日志（默认强制鉴权、
	 *              零配置可用）
	 */
	public ReloadClassServer(@NotNull AppBase app,
							 @NotNull String urlPath,
							 @NotNull String uploadDir,
							 @NotNull String fileVarName,
							 @Nullable String token) {
		this.uploadDir = uploadDir;
		this.fileVarName = fileVarName;
		this.token = token != null && !token.isBlank() ? token : generateToken();
		logger.warn("ReloadClassServer '{}' enabled: hot-reload endpoint requires token"
						+ " (pass via '{}' header or '{}' query param). token={}",
				urlPath, TOKEN_HEADER, TOKEN_QUERY_KEY, this.token);
		assert app.getHttpServer() != null;
		app.getHttpServer().addHandler(urlPath, TransactionLevel.None, DispatchMode.Normal, this);
	}

	static @NotNull String generateToken() {
		var bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	/** token校验（FND8-66）：X-Zeze-Token头或token查询参数携带，MessageDigest常量时间比较。 */
	static boolean checkToken(@NotNull String token, @NotNull HttpExchange x) {
		var request = x.request();
		var presented = request != null ? request.headers().get(TOKEN_HEADER) : null;
		if (presented == null)
			presented = x.queryMap().get(TOKEN_QUERY_KEY);
		return presented != null && MessageDigest.isEqual(
				token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public @NotNull String getFileNameQueryKey() {
		return fileVarName;
	}

	public void start() throws Exception {
		var files = new File(uploadDir).listFiles();
		if (null == files || files.length == 0)
			return;
		if (files.length != 1)
			throw new RuntimeException("too many patch file.");
		try (var zipFile = new ZipFile(files[0])) {
			ClassReloader.reloadClasses(zipFile);
		}
	}

	@Override
	public void onEndRequest(@NotNull HttpExchange x,
							 @NotNull InterfaceHttpPostRequestDecoder decoder) throws Exception {
		// 鉴权前置（FND8-66）：失配403并记录来源，不进入热更路径
		if (!checkToken(token, x)) {
			logger.warn("ReloadClassServer: reject unauthorized reload request from {}", x.channel().remoteAddress());
			x.close(x.sendPlainText(HttpResponseStatus.FORBIDDEN, "forbidden"));
			return;
		}
		// 字段守卫（FND8-67）：multipart可缺字段（getBodyHttpData返回null）或放同名文本字段
		// （返回MemoryAttribute），原无守卫强转分别NPE/CCE且异常穿透后请求无应答挂起；
		// instanceof模式匹配同时覆盖两形态，按400明确拒绝（与sanitize拒绝路径同口径）。
		var data = decoder.getBodyHttpData(getFileNameQueryKey());
		if (!(data instanceof FileUpload fileUpload)) {
			logger.warn("Reject upload: missing or invalid file field '{}'", getFileNameQueryKey());
			x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST,
					"missing or invalid file field '" + getFileNameQueryKey() + "'"));
			return;
		}
		var patchFileName = fileUpload.getFilename();
		new File(uploadDir).mkdirs();
		final File destFile; // 落盘路径必须经净化（FND4-70）：客户端可控文件名不得携带目录成分
		try {
			destFile = HttpFileUploadHandle.sanitizeDestFile(new File(uploadDir), patchFileName);
		} catch (IllegalArgumentException e) {
			logger.warn("Reject upload filename '{}' (path traversal)", patchFileName);
			x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, "illegal filename"));
			return;
		}
		destFile.delete(); // 只保存一份path_all; skip result.
		if (fileUpload.renameTo(destFile)) {
			try (var zipFile = new ZipFile(destFile)) {
				ClassReloader.reloadClasses(zipFile);
			}
			// 审计（FND8-66）：每次成功使用的留痕
			logger.info("ReloadClassServer: authorized hot-reload from {}, file='{}'",
					x.channel().remoteAddress(), patchFileName);
			x.close(x.sendPlainText(HttpResponseStatus.OK, ""));
			return;
		}
		x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, ""));
	}
}
