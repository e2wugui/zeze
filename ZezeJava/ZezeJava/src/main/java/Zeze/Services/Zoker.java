package Zeze.Services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Builtin.Zoker.ListService;
import Zeze.Config;
import Zeze.Services.ZokerImpl.DistributeManager;
import Zeze.Services.ZokerImpl.ServiceManager;
import Zeze.Services.ZokerImpl.ZokerService;

/**
 * 服务分发管理端：接收 ZokerAgent 上传的服务文件，提交为版本目录，并管理服务进程的启停。
 */
public class Zoker extends AbstractZoker {
	private static final Logger logger = LogManager.getLogger(Zoker.class);

	private final ZokerService serverWithConnector; // Zoker是server，但它主动连接ZokerAgent
	private final DistributeManager distributeManager;
	private final ServiceManager processManager;
	private final File serviceDir;
	private final File distributeDir;
	private final File zokerDir;

	public Zoker(Config config, String baseDir) throws IOException {
		// init/create dir
		zokerDir = new File(baseDir);
		Files.createDirectories(zokerDir.toPath());
		// services/ 是版本容器布局：services/<svc>/<versionNo>/... + services/<svc>/current 指针。
		// 不创建旧布局的 servicesOld/ 备份目录（回滚点由保留的版本目录承担）。
		serviceDir = Path.of(baseDir, "services").toFile();
		Files.createDirectories(serviceDir.toPath());
		distributeDir = Path.of(baseDir, "distributes").toFile();
		Files.createDirectories(distributeDir.toPath());

		// implement
		distributeManager = new DistributeManager(this);
		serverWithConnector = new ZokerService(config, distributeManager);
		processManager = new ServiceManager(this);
		RegisterProtocols(serverWithConnector);
	}

	public File getZokerDir() {
		return zokerDir;
	}

	public File getServiceDir() {
		return serviceDir;
	}

	public File getDistributeDir() {
		return distributeDir;
	}

	public void start() throws Exception {
		// listen 前启动对账领养——上一代 Zoker 留下的孤儿先按 run.pid 身份核实
		// 装账，start/stop/list 从第一帧起即跨 Zoker 重启连续（对账点=启动扫描，单一入口）。
		processManager.adoptOrphans();
		serverWithConnector.start();
	}

	public void stop() throws Exception {
		serverWithConnector.stop();
		distributeManager.closeAll();
	}

	// 三个文件handler的异常必须映射回协议声明的错误码（eOpenError/eAppendOffset/eCloseError）：
	// 上抛给协议框架不会回结果，客户端只能等满5秒RPC超时，错误分类信息全部丢失。
	@Override
	protected long ProcessOpenFileRequest(Zeze.Builtin.Zoker.OpenFile r) throws Exception {
		try {
			var fileBin = distributeManager.open(r.Argument.getServiceName(), r.Argument.getFileName(), r.getSender());
			r.Result.setOffset(fileBin.getLength());
		} catch (Exception e) {
			logger.error("OpenFile {}/{}", r.Argument.getServiceName(), r.Argument.getFileName(), e);
			return errorCode(eOpenError);
		}
		r.SendResult();
		return 0;
	}

	// AppendFile的异常统一映射eAppendOffset（协议未声明其他append错误码）：无论越界还是未打开，
	// 客户端的恢复动作相同——重新OpenFile取长度后重传。
	@Override
	protected long ProcessAppendFileRequest(Zeze.Builtin.Zoker.AppendFile r) throws Exception {
		try {
			distributeManager.append(r.Argument.getServiceName(),
					r.Argument.getFileName(),
					r.Argument.getOffset(), r.Argument.getChunk());
		} catch (Exception e) {
			logger.error("AppendFile {}/{} offset={}", r.Argument.getServiceName(), r.Argument.getFileName(),
					r.Argument.getOffset(), e);
			return errorCode(eAppendOffset);
		}
		r.SendResult();
		return 0;
	}

	// CloseFile 错误码路径（三态）：eCloseError=系统异常；eNotOpened=文件不在传输中
	// （未Open/已收尾/断链回收后补发——agent重连补发的close走此路径，不谎报校验成功）；
	// eMd5Mismatch=校验失败（服务端已删除损坏中间产物，重传从0开始）。
	@Override
	protected long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) throws Exception {
		long rc;
		try {
			rc = distributeManager.closeAndVerify(r.Argument.getServiceName(),
					r.Argument.getFileName(), r.Argument.getMd5(), r.getSender());
		} catch (Exception e) {
			logger.error("CloseFile {}/{}", r.Argument.getServiceName(), r.Argument.getFileName(), e);
			return errorCode(eCloseError);
		}
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCommitServiceRequest(Zeze.Builtin.Zoker.CommitService r) {
		return distributeManager.commitService(r);
	}

	@Override
	protected long ProcessListServiceRequest(ListService r) {
		processManager.listService(r.Result.getServices());
		r.SendResult();
		return 0;
	}

	// StartService 的失败映射协议错误码：eNoServiceProperties=缺少部署描述文件
	// service.properties（含无现役版本）；eStartFail=进程创建失败。不异常上抛（无结果包=客户端超时）。
	@Override
	protected long ProcessStartServiceRequest(Zeze.Builtin.Zoker.StartService r) {
		var rc = processManager.startService(r);
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessStopServiceRequest(Zeze.Builtin.Zoker.StopService r) throws Exception {
		processManager.stopService(r);
		r.SendResult();
		return 0;
	}
}
