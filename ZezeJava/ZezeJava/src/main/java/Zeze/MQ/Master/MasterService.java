package Zeze.MQ.Master;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Util.TaskSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MQ Master 的网络服务：socket 关闭时联动摘除对应的 Manager 注册条目。
 */
public class MasterService extends Service {
	private final Main main;

	public MasterService(Main main, Config config) {
		super("Zeze.MQ.Master", config);
		setNoProcedure(true);
		this.main = main;
	}

	@Override
	public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
		super.OnSocketClose(so, e);
		// 本回调在发起关闭的线程（网络侧关闭=selector 线程；Service.stop 持 Service 锁关全部
		// socket 时为 stop 线程）同步执行，不得在此等待 Master 模块锁——模块锁临界区含阻塞
		// rpc（CreateMQ 的 CreatePartition、孤儿对账的 DeletePartition），selector 被锁阻塞
		// 即 Master 全部连接的读写/accept 停摆，且锁持有者等待的应答恰好需要该 selector 读取。
		// 摘除注册条目提交任务池异步执行（对齐 MQManager.Service.OnMasterConnected 的
		// "IO线程不等待"写法）；重连 Register 按幂等替换旧条目，不依赖摘除先于重注册完成。
		TaskSpec.ofAction(() -> main.getMaster().tryRemoveManager(so))
				.name("Zeze.MQ.Master.tryRemoveManager")
				.submitNow();
	}
}
