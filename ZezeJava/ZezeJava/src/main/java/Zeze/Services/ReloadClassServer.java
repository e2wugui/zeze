package Zeze.Services;

import java.io.File;
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

public class ReloadClassServer implements HttpFileUploadHandle {
	private final String uploadDir;
	private final String fileVarName;

	/**
	 * 构造ReloadClassServer
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
		this.uploadDir = uploadDir;
		this.fileVarName = fileVarName;
		assert app.getHttpServer() != null;
		app.getHttpServer().addHandler(urlPath, TransactionLevel.None, DispatchMode.Normal, this);
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
		var zipFile = new ZipFile(files[0]);
		ClassReloader.reloadClasses(zipFile);
	}

	@Override
	public void onEndRequest(@NotNull HttpExchange x,
							 @NotNull InterfaceHttpPostRequestDecoder decoder) throws Exception {
		var fileUpload = (FileUpload)decoder.getBodyHttpData(getFileNameQueryKey());
		var patchFileName = fileUpload.getFilename();
		new File(uploadDir).mkdirs();
		var destFile = new File(uploadDir, patchFileName);
		destFile.delete(); // 只保存一份path_all; skip result.
		if (fileUpload.renameTo(destFile)) {
			var zipFile = new ZipFile(destFile);
			ClassReloader.reloadClasses(zipFile);
			x.close(x.sendPlainText(HttpResponseStatus.OK, ""));
			return;
		}
		x.close(x.sendPlainText(HttpResponseStatus.BAD_REQUEST, ""));
	}
}
