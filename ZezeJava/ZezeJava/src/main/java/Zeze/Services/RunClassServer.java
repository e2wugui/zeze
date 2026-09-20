package Zeze.Services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import Zeze.AppBase;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpFileUploadHandle;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 【安全警示】任意字节码执行端点：上传的class字节码被直接defineClass并实例化执行
 * （Runnable/Callable/main），触达且持有token者即获得服务器任意代码执行权，仍必须仅
 * 绑定回环/内网，绝不可暴露公网。token校验（FND8-66）把"部署纪律"升级为代码不变量：
 * 构造时显式传入token，或传null由启动期自动生成随机token并打日志（零配置可用，运维
 * 从日志取token）；请求须以X-Zeze-Token头或"token"查询参数携带，常量时间比较，失配
 * 答403。
 */
public class RunClassServer implements HttpFileUploadHandle {
	private static final @NotNull org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(RunClassServer.class);
	private final String uploadDir;
	private final String fileVarName;
	private final String token;

	/**
	 * 构造RunClassServer（token自动生成，见类注释）。
	 *
	 * @param app         应用App实例
	 * @param urlPath     上传文件的urlPath
	 * @param uploadDir   上传文件的目录
	 * @param fileVarName 上传文件名查询Key，
	 *                    比如直接上传的url:/upload?fileVarName=xxx
	 *                    或者表单中&lt;input type="file" name="fileVarName" multiple&gt;
	 */
	public RunClassServer(@NotNull AppBase app,
						  @NotNull String urlPath,
						  @NotNull String uploadDir,
						  @NotNull String fileVarName) {
		this(app, urlPath, uploadDir, fileVarName, null);
	}

	/**
	 * 构造RunClassServer。
	 *
	 * @param token 鉴权token；null/空白时自动生成随机token并打日志（默认强制鉴权、
	 *              零配置可用）
	 */
	public RunClassServer(@NotNull AppBase app,
						  @NotNull String urlPath,
						  @NotNull String uploadDir,
					  @NotNull String fileVarName,
					  @Nullable String token) {
		this.uploadDir = uploadDir;
		this.fileVarName = fileVarName;
		this.token = token != null && !token.isBlank() ? token : ReloadClassServer.generateToken();
		logger.warn("RunClassServer '{}' enabled: run-class endpoint requires token"
						+ " (pass via '{}' header or '{}' query param). token={}",
				urlPath, ReloadClassServer.TOKEN_HEADER, ReloadClassServer.TOKEN_QUERY_KEY, this.token);
		assert app.getHttpServer() != null;
		app.getHttpServer().addHandler(urlPath, TransactionLevel.None, DispatchMode.Normal, this);
	}

	@Override
	public @NotNull String getFileNameQueryKey() {
		return fileVarName;
	}

	@Override
	public void onEndRequest(@NotNull HttpExchange x,
							 @NotNull InterfaceHttpPostRequestDecoder decoder) throws Exception {
		var fileUpload = ReloadClassServer.checkTokenAndTakeUpload(logger, "RunClassServer", "run",
				token, x, decoder, getFileNameQueryKey());
		if (fileUpload == null)
			return;
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
		destFile.delete(); // run class 总是覆盖; skip result.
		if (fileUpload.renameTo(destFile)) {
			var path = destFile.toPath();
			var classBytes = Files.readAllBytes(path);
			var result = "";
			// S3-F3：执行段三分支（Runnable/Callable/main）与defineClass原均无守卫——
			// getMethod/实例化/invoke/解码失败异常穿透，请求无HTTP应答即断连（同方法其余
			// 拒绝路径均有显式应答）。catch用Throwable而非Exception：defineClass对损坏
			// 字节码抛ClassFormatError（Error子类），仅Exception盖不住最常见触发形态。
			try {
				var classLoader = new BytecodeClassLoader();
				var loadClass = classLoader.defineClass(classBytes);
				if (Runnable.class.isAssignableFrom(loadClass)) {
					var instance = loadClass.getConstructor().newInstance();
					((Runnable)instance).run();
				} else if (Callable.class.isAssignableFrom(loadClass)) {
					var instance = loadClass.getConstructor().newInstance();
					result = String.valueOf(((Callable<?>)instance).call());
				} else {
					var mainMethod = loadClass.getMethod("main", String[].class);
					var args = decoder.isMultipart() ? getArgs(decoder) : getArgs(x);
					result = String.valueOf(mainMethod.invoke(null, (Object)args));
				}
			} catch (Throwable e) { // logger.error
				logger.error("RunClassServer: run failed, file='{}'", patchFileName, e);
				x.close(x.sendPlainText(HttpResponseStatus.INTERNAL_SERVER_ERROR, "run failed: " + e));
				return;
			}
			// 审计（FND8-66）：每次成功使用的留痕
			logger.info("RunClassServer: authorized run from {}, file='{}'",
					x.channel().remoteAddress(), patchFileName);
			x.close(x.sendPlainText(HttpResponseStatus.OK, result));
			return;
		}
		x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, "Bad Request"));
	}

	private static @NotNull String @NotNull [] getArgs(@NotNull HttpExchange x) {
		var args = new ArrayList<String>();
		var queryMap = x.queryMap();
		for (int i = 0; ; i++) {
			var value = queryMap.get("arg" + i);
			if (value == null)
				return args.toArray(new String[0]);
			args.add(value);
		}
	}

	private static @NotNull String @NotNull [] getArgs(@NotNull InterfaceHttpPostRequestDecoder decoder) throws IOException {
		var args = new ArrayList<String>();
		for (int i = 0; ; i++) {
			var httpData = decoder.getBodyHttpData("arg" + i);
			if (!(httpData instanceof Attribute))
				return args.toArray(new String[0]);
			args.add(((Attribute)httpData).getValue());
		}
	}

	static class BytecodeClassLoader extends ClassLoader {
		public Class<?> defineClass(byte[] bytecode) {
			// 核心方法：将字节数组转换为Class对象
			return defineClass(null, bytecode, 0, bytecode.length);
		}
	}
}
