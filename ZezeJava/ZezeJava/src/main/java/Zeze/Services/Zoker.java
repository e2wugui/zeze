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
		// services/ 是版本容器布局（GE-D02）：services/<svc>/<versionNo>/... + services/<svc>/current 指针。
		// 旧布局的 servicesOld/ 备份目录概念随版本目录回滚点一并消失，不再创建。
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

	@Override
	protected long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) throws Exception {
		boolean verify;
		try {
			verify = distributeManager.closeAndVerify(r.Argument.getServiceName(),
					r.Argument.getFileName(), r.Argument.getMd5(), r.getSender());
		} catch (Exception e) {
			logger.error("CloseFile {}/{}", r.Argument.getServiceName(), r.Argument.getFileName(), e);
			return errorCode(eCloseError);
		}
		if (!verify)
			return errorCode(eMd5Mismatch);
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

	@Override
	protected long ProcessStartServiceRequest(Zeze.Builtin.Zoker.StartService r) {
		processManager.startService(r);
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
