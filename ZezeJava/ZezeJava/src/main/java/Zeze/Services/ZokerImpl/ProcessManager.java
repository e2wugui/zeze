package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.Util.Task;

public class ProcessManager {
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

	public ProcessManager() {

	}

	public void listService(String baseDir, java.util.ArrayList<Zeze.Builtin.Zoker.BService.Data> out) {
		var listFiles = new File(baseDir).listFiles();
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

	private static java.util.List<String> buildCommand(String serviceName) {
		return new ArrayList<>(); // todo 需要确定服务进程启动规范(service.xml?或脚本)
	}

	private static Process newProcess(String serviceName) {
		var pb = new ProcessBuilder();
		pb.command(buildCommand(serviceName));
		try {
			return pb.start();
		} catch (IOException e) {
			Task.forceThrow(e);
		}
		return null; // never got here
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

		// todo waitFor timeout? 1 超时时间由客户都安控制？2 超时返回结果，恢复processes（上面remove了）
		if (r.Argument.isForce())
			return process.destroyForcibly().waitFor();

		if (supportsNormalTermination(process)) {
			process.destroy();
			return process.waitFor();
		}

		// todo 不支持温和退出的时候怎么办。
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
