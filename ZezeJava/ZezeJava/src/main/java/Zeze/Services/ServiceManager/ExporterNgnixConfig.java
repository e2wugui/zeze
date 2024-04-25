package Zeze.Services.ServiceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ExporterNgnixConfig implements IExporter {
	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
		var lines = new ArrayList<String>();
		var hasChanged = false;
		try (var config = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			String line;
			var skipUntilUpstreamEnd = false;
			while ((line = config.readLine()) != null) {
				if (skipUntilUpstreamEnd && !line.trim().equals("}"))
					continue;

				if (line.trim().startsWith("upstream")) {
					var sName = line.split(" ")[1];
					if (sName.equals(serviceName)) {
						skipUntilUpstreamEnd = true;
						exportToLines(lines, serviceName, all);
						hasChanged = true;
						continue;
					}
				}
				lines.add(line);
			}
		}
		if (hasChanged) {
			var sb = new StringBuilder();
			for (var line : lines)
				sb.append(line).append("\n");
			Files.writeString(Path.of(file), sb.toString());
		}
	}

	private static void exportToLines(ArrayList<String> out, String serviceName, BServiceInfosVersion all) {
		out.add("upstream " + serviceName + " {");
		for (var info : all.getInfosVersion().get(0).getServiceInfoListSortedByIdentity()) {
			if (info.getPassiveIp().isBlank())
				continue;
			out.add("    server " + info.getPassiveIp() + ":" + info.getPassivePort() + ";");
		}
		out.add("}");
	}

	private final String file;

	/**
	 * 构造Ngnix配置文件输出器。
	 * 当SM信息发生变化，会把服务列表输出到配置文件。
	 *
	 * @param param 配置文件路径名字
	 */
	public ExporterNgnixConfig(String param) {
		this.file = param;
	}
}
