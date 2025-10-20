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
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.jetbrains.annotations.NotNull;

public class RunClassServer implements HttpFileUploadHandle {
	private final String uploadDir;
	private final String fileVarName;

	/**
	 * 构造RunClassServer
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
		this.uploadDir = uploadDir;
		this.fileVarName = fileVarName;
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
		var fileUpload = (FileUpload)decoder.getBodyHttpData(getFileNameQueryKey());
		var patchFileName = fileUpload.getFilename();
		new File(uploadDir).mkdirs();
		var destFile = new File(uploadDir, patchFileName);
		destFile.delete(); // run class 总是覆盖; skip result.
		if (fileUpload.renameTo(destFile)) {
			var path = destFile.toPath();
			var classBytes = Files.readAllBytes(path);
			var classLoader = new BytecodeClassLoader();
			var loadClass = classLoader.defineClass(classBytes);
			var result = "";
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
				return args.toArray(new String[args.size()]);
			args.add(value);
		}
	}

	private static @NotNull String @NotNull [] getArgs(@NotNull InterfaceHttpPostRequestDecoder decoder) throws IOException {
		var args = new ArrayList<String>();
		for (int i = 0; ; i++) {
			var httpData = decoder.getBodyHttpData("arg" + i);
			if (!(httpData instanceof Attribute))
				return args.toArray(new String[args.size()]);
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
