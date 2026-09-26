package Zeze.Services.ZokerImpl;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZokerService extends Service {
	private final DistributeManager distributeManager;

	public ZokerService(Config config, DistributeManager distributeManager) {
		super("Zeze.ZokerService", config);
		this.distributeManager = distributeManager;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		// agent断链即整批回收该连接打开的FileBin：断链早于CloseFile时RandomAccessFile无任何
		// 超时清理路径——句柄常驻泄漏，Windows上锁住distributes下的文件使commit的rename失败。
		distributeManager.closeBySocket(so);
		super.OnSocketClose(so, e);
	}
}
