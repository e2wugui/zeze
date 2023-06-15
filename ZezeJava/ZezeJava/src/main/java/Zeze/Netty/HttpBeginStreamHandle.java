package Zeze.Netty;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HttpBeginStreamHandle {
	// from,to,size是从HTTP头中的Content-Range字段取得的,通常用于上传文件,缺省值为-1
	void onBeginStream(@NotNull HttpExchange x, long from, long to, long size) throws Exception;
}
