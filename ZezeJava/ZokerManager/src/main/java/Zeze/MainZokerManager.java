package Zeze;

import Zeze.Config;
import Zeze.Services.LogService;
import Zeze.Util.Task;
import Zeze.log.LogAgentManager;
import Zeze.log.ZokerManagerConf;

/**
 * ZokerManager 进程入口：启动 LogService 与日志查询代理（含 HTTP 服务）。
 */
public class MainZokerManager {
	public static void main(String[] args) {
		var configXml = "server.xml";
		try {
			start(configXml);
		} catch (Throwable e) {
			// start 已回收全部已启动组件：以非零码退出，与部署契约校验的 fail-fast
			// 姿态对齐——不残留"LogService 照常服务、管理口无人监听"的半启动僵尸
			//（依赖进程退出动作的守护重启才能触发）。
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 启动 LogService 与日志查询代理（含 HTTP 服务），启动序列任一步失败按逆序
	 * 回收已启动组件（LogAgentManager → LogService）后上抛——不留半启动状态
	 * （嵌入宿主形态拿到异常自行处置；进程形态由 main 转 exit）。
	 */
	public static void start(String configXml) throws Exception {
		Task.tryInitThreadPool();

		var conf = new ZokerManagerConf();
		// 部署契约（FND29 zokermanager-02）先于一切服务启动校验：非回环 Bind 且未配
		// Token 直接抛错退出，不启动 LogService/LogAgent——避免 fail-fast 后残留半启动线程。
		Config.load(configXml).parseCustomize(conf);
		ZokerManagerConf.checkDeployPolicy(conf.bind, conf.token);

		var logService = new LogService(Config.load(configXml));
		try {
			logService.start();
			LogAgentManager.init(configXml);
		} catch (Throwable e) {
			// 半启动回收：LogService 构造即启动文件监视线程（原为非守护，zokermanager-02
			// 起 daemon 化）并占 (logDir, logActive) 独占登记；init 失败（bind 冲突/SM
			// waitReady 双败等）若不回收，进程被钉成半启动僵尸且重启撞独占登记。
			// 逆序回收，回收失败不掩盖原始异常。
			try {
				LogAgentManager.stop();
			} catch (Throwable stopEx) {
				e.addSuppressed(stopEx);
			}
			try {
				logService.stop();
			} catch (Throwable stopEx) {
				e.addSuppressed(stopEx);
			}
			throw e;
		}
	}
}
