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
import org.jspecify.annotations.Nullable;

/**
 * HTTP 类热更新端点：接收上传的 zip 补丁包并经 {@link ClassReloader} 替换已加载类。
 * 【安全警示】本端点等价于任意代码执行，必须仅绑定回环/内网并强制 token 鉴权（经 X-Zeze-Token 头或 token 参数传递）。
 */
public class ReloadClassServer implements HttpFileUploadHandle {
	private static final org.apache.logging.log4j.Logger logger =
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
	public ReloadClassServer(AppBase app,
							 String urlPath,
							 String uploadDir,
							 String fileVarName) {
		this(app, urlPath, uploadDir, fileVarName, null);
	}

	/**
	 * 构造ReloadClassServer。
	 *
	 * @param token 鉴权token；null/空白时自动生成随机token并打日志（默认强制鉴权、
	 *              零配置可用）
	 */
	public ReloadClassServer(AppBase app,
							 String urlPath,
							 String uploadDir,
							 String fileVarName,
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

	static String generateToken() {
		var bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	/** token校验：X-Zeze-Token头或token查询参数携带，MessageDigest常量时间比较。 */
	static boolean checkToken(String token, HttpExchange x) {
		var request = x.request();
		var presented = request != null ? request.headers().get(TOKEN_HEADER) : null;
		if (presented == null)
			presented = x.queryMap().get(TOKEN_QUERY_KEY);
		return presented != null && MessageDigest.isEqual(
				token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
	}

	/** ReloadClassServer/RunClassServer的onEndRequest共用前置守卫：
	 * token鉴权+multipart字段校验。拒绝路径已代回应答并关闭exchange（403/400），
	 * 返回null；通过时返回解出的FileUpload。owner/action仅用于403日志归因。 */
	static @Nullable FileUpload checkTokenAndTakeUpload(org.apache.logging.log4j.Logger logger,
														String owner, String action,
														String token, HttpExchange x,
														InterfaceHttpPostRequestDecoder decoder,
														String fileVarName) {
		// 鉴权失败直接回 403
		if (!checkToken(token, x)) {
			logger.warn("{}: reject unauthorized {} request from {}", owner, action, x.channel().remoteAddress());
			x.close(x.sendPlainText(HttpResponseStatus.FORBIDDEN, "forbidden"));
			return null;
		}
		// 校验 multipart 文件字段类型，缺字段或为普通文本字段时回 400
		var data = decoder.getBodyHttpData(fileVarName);
		if (!(data instanceof FileUpload fileUpload)) {
			logger.warn("Reject upload: missing or invalid file field '{}'", fileVarName);
			x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST,
					"missing or invalid file field '" + fileVarName + "'"));
			return null;
		}
		return fileUpload;
	}

	@Override
	public String getFileNameQueryKey() {
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
	public void onEndRequest(HttpExchange x,
							 InterfaceHttpPostRequestDecoder decoder) throws Exception {
		var fileUpload = checkTokenAndTakeUpload(logger, "ReloadClassServer", "reload",
				token, x, decoder, getFileNameQueryKey());
		if (fileUpload == null)
			return;
		var patchFileName = fileUpload.getFilename();
		//noinspection ResultOfMethodCallIgnored
		new File(uploadDir).mkdirs();
		final File destFile; // 落盘路径必须经净化：客户端可控文件名不得携带目录成分
		try {
			destFile = HttpFileUploadHandle.sanitizeDestFile(new File(uploadDir), patchFileName);
		} catch (IllegalArgumentException e) {
			logger.warn("Reject upload filename '{}' (path traversal)", patchFileName);
			x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, "illegal filename"));
			return;
		}
		//noinspection ResultOfMethodCallIgnored
		destFile.delete(); // 只保存一份path_all; skip result.
		if (fileUpload.renameTo(destFile)) {
			// 热更或解压失败时删除坏补丁并回 500，保证目录内只留可用补丁（不阻断下次启动）
			try (var zipFile = new ZipFile(destFile)) {
				ClassReloader.reloadClasses(zipFile);
			} catch (Throwable e) {
				logger.error("ReloadClassServer: hot-reload failed, delete patch file '{}'", destFile, e);
				//noinspection ResultOfMethodCallIgnored
				destFile.delete();
				x.close(x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, "hot-reload failed"));
				return;
			}
			// 维持目录内仅保留当前一份补丁，避免重启 start() 发现多份补丁失败
			cleanupOtherPatchFiles(new File(uploadDir), destFile);
			logger.info("ReloadClassServer: authorized hot-reload from {}, file='{}'",
					x.channel().remoteAddress(), patchFileName);
			x.close(x.sendPlainText(HttpResponseStatus.OK, ""));
			return;
		}
		x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, ""));
	}

	/** 维持start()的"目录内唯一补丁"不变式：成功热更后清理目录内其他文件（含不同文件名的历史补丁）。 */
	static void cleanupOtherPatchFiles(File dir, File keep) {
		var files = dir.listFiles();
		if (files == null)
			return;
		for (var f : files) {
			if (f.equals(keep) || !f.isFile())
				continue;
			if (!f.delete())
				logger.warn("ReloadClassServer: cleanup old patch file failed: {}", f);
		}
	}
}
