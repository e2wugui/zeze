package Zeze.Services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Zoker.ListService;
import Zeze.Config;
import Zeze.Services.ZokerImpl.DistributeManager;
import Zeze.Services.ZokerImpl.ServiceManager;
import Zeze.Services.ZokerImpl.ZokerService;

public class Zoker extends AbstractZoker {
	private final ZokerService serverWithConnector; // Zoker是server，但它主动连接ZokerAgent
	private final DistributeManager distributeManager;
	private final ServiceManager processManager;
	private final File serviceDir;
	private final File distributeDir;
	private final File serviceOldDir;
	private final File zokerDir;

	public Zoker(Config config, String baseDir) throws IOException {
		serverWithConnector = new ZokerService(config);

		// init/create dir
		zokerDir = new File(baseDir);
		Files.createDirectories(zokerDir.toPath());
		serviceDir = Path.of(baseDir, "services").toFile();
		Files.createDirectory(serviceDir.toPath());
		distributeDir = Path.of(baseDir, "distributes").toFile();
		Files.createDirectory(distributeDir.toPath());
		serviceOldDir = Path.of(baseDir, "servicesOld").toFile();
		Files.createDirectory(serviceOldDir.toPath());

		// implement
		distributeManager = new DistributeManager(this);
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

	public File getServiceOldDir() {
		return serviceOldDir;
	}

	public void start() throws Exception {
		serverWithConnector.start();
	}

	public void stop() throws Exception {
		serverWithConnector.start();
	}

	@Override
	protected long ProcessOpenFileRequest(Zeze.Builtin.Zoker.OpenFile r) throws Exception {
		var fileBin = distributeManager.open(r.Argument.getServiceName(), r.Argument.getFileName());
		r.Result.setOffset(fileBin.getLength());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessAppendFileRequest(Zeze.Builtin.Zoker.AppendFile r) throws Exception {
		distributeManager.append(r.Argument.getServiceName(),
				r.Argument.getFileName(),
				r.Argument.getOffset(), r.Argument.getChunk());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessCloseFileRequest(Zeze.Builtin.Zoker.CloseFile r) throws Exception {
		if (!distributeManager.closeAndVerify(r.Argument.getServiceName(),
				r.Argument.getFileName(), r.Argument.getMd5()))
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
