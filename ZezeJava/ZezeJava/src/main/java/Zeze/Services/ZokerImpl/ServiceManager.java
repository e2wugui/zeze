package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.Services.Zoker;
import Zeze.Util.Task;

public class ServiceManager {
	private final Zoker zoker;
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

	public ServiceManager(Zoker zoker) {
		this.zoker = zoker;
	}

	public Zoker getZoker() {
		return zoker;
	}

	public void listService(ArrayList<Zeze.Builtin.Zoker.BService.Data> out) {
		var listFiles = zoker.getServiceDir().listFiles();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isDirectory()) {
					var service = new BService.Data();
					service.setServiceName(file.getName());
					var process = processes.get(service.getServiceName());
					service.setState(null != process ? "running" : "");
					if (null != process)
						service.setPs(process.info().toString());
					out.add(service);
				}
			}
		}
	}

	private static List<String> buildCommand(@SuppressWarnings("unused") String serviceName) {
		return new ArrayList<>();
	}

	private Process newProcess(String serviceName) {
		var pb = new ProcessBuilder();
		pb.directory(new File(zoker.getServiceDir(), serviceName));
		pb.command(buildCommand(serviceName));
		try {
			return pb.start();
		} catch (IOException e) {
			Task.forceThrow(e);
		}
		return null; // never run here
	}

	public void startService(StartService r) {
		var serviceName = r.Argument.getServiceName();
		var process = processes.computeIfAbsent(serviceName, __ -> newProcess(serviceName));
		r.Result.setServiceName(serviceName);
		r.Result.setState("running");
		assert process != null;
		r.Result.setPs(process.info().toString());
	}

	public int stopService(StopService r) throws InterruptedException {
		var serviceName = r.Argument.getServiceName();
		var process = processes.remove(serviceName);
		if (null == process)
			return 0;

		r.Result.setServiceName(serviceName);
		r.Result.setState("");
		r.Result.setPs(process.info().toString());

		// waitFor timeout? 1 超时时间由客户都安控制？2 超时返回结果，恢复processes（上面remove了）
		if (r.Argument.isForce())
			return process.destroyForcibly().waitFor();

		if (supportsNormalTermination(process)) {
			process.destroy();
			return process.waitFor();
		}

		return process.destroyForcibly().waitFor();
	}

	private static boolean supportsNormalTermination(Process process) {
		try {
			return process.supportsNormalTermination();
		} catch (Exception ex) {
			return false;
		}
	}
}
