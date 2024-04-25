package Zeze.Services.ServiceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public class ExporterNginxConfig implements IExporter {
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
				var lineTrim = line.trim();
				if (skipUntilUpstreamEnd) {
					if (!lineTrim.equals("}"))
						continue; // skip
					skipUntilUpstreamEnd = false;
					continue; // skip last "}"
				}

				if (lineTrim.startsWith("upstream")) {
					var prefix = line.substring(0, line.length() - lineTrim.length());
					var sName = lineTrim.split(" ")[1];
					if (sName.equals(serviceName)) {
						skipUntilUpstreamEnd = true;
						exportToLines(prefix, lines, serviceName, all);
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
			//System.out.println(sb);
			Files.writeString(Path.of(file), sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	private void exportToLines(String prefix, ArrayList<String> out, String serviceName, BServiceInfosVersion all) {
		out.add(prefix + "upstream " + serviceName + " {");
		var ver0  = all.getInfosVersion().get(version);
		if (null != ver0) {
			for (var info : ver0.getServiceInfoListSortedByIdentity()){
				if (info.getPassiveIp().isBlank())
					continue;
				out.add(prefix + "    server " + info.getPassiveIp() + ":" + info.getPassivePort() + ";");
			}
		}
		out.add(prefix + "}");
	}

	private final String file;
	private final long version;

	/**
	 * 构造Ngnix配置文件输出器。
	 * 当SM信息发生变化，会把服务列表输出到配置文件。
	 *
	 * @param param 配置文件路径名字
	 */
	public ExporterNginxConfig(@NotNull String param, @NotNull String param2) {
		this.file = param;
		this.version = Long.parseLong(param2);
	}
}
