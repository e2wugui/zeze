package Zeze.Services.ServiceManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public class ExporterNginxConfig implements IExporter {
	private static final @NotNull org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(ExporterNginxConfig.class);

	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
		var lines = new ArrayList<String>();
		var hasChanged = false;
		var found = false;
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
					// FND5-36：nginx合法写法多样（"upstream name{"、"upstream<TAB>name {"）——原
					// split(" ")[1]要么解析出带'{'的错名（块永不重写，下线地址残留），要么
					// AIOOBE中断整批导出。按空白切分取第二token去尾'{'；解析不出名字记告警跳过。
					var tokens = lineTrim.split("\\s+");
					var sName = tokens.length > 1 ? trimSuffixBrace(tokens[1]) : "";
					if (sName.isEmpty()) {
						logger.warn("ExporterNginxConfig: unrecognized upstream line skipped: {}", lineTrim);
					} else if (sName.equals(serviceName)) {
						skipUntilUpstreamEnd = true;
						exportToLines(prefix, lines, serviceName, all);
						hasChanged = true;
						found = true;
						continue;
					}
				}
				lines.add(line);
			}
		}
		// FND6-30：整文件未命中同名upstream块时原实现恒不更新（hasChanged恒false）——新增服务
		// 或首次部署未预置空块时该服务地址永不进nginx、无自愈无告警（未文档化的隐含契约）。
		// 改为文件尾追加自产格式块（含服务暂无identity的空块态，与已存在块的重写语义一致），
		// 后续轮转可正常识别重写。
		if (!found) {
			exportToLines("", lines, serviceName, all);
			hasChanged = true;
			logger.info("ExporterNginxConfig: upstream block for '{}' not found, append to end of {}",
					serviceName, file);
		}
		if (hasChanged) {
			var sb = new StringBuilder();
			for (var line : lines)
				sb.append(line).append("\n");
			//System.out.println(sb);
			Files.writeString(Path.of(file), sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
			reload();
		}
	}

	private static String trimSuffixBrace(String token) {
		return token.endsWith("{") ? token.substring(0, token.length() - 1) : token;
	}

	private void exportToLines(String prefix, ArrayList<String> out, String serviceName, BServiceInfosVersion all) {
		out.add(prefix + "upstream " + serviceName + " {");
		var ver0 = all.getInfos(version);
		if (null != ver0) {
			for (var info : ver0.getSortedIdentities()) {
				if (info.getPassiveIp().isBlank())
					continue;
				out.add(prefix + "    server " + info.getPassiveIp() + ":" + info.getPassivePort() + ";");
			}
		}
		out.add(prefix + "}");
	}

	private final String file;
	private final long version;
	private final String reload;

	@SuppressWarnings("deprecation")
	private void reload() throws IOException {
		if (null == reload || reload.isBlank())
			return;

		Runtime.getRuntime().exec(reload);
	}

	/**
	 * 构造Nginx配置文件输出器。
	 * 当SM信息发生变化，会把服务列表输出到配置文件。
	 */
	public ExporterNginxConfig(@NotNull ExporterConfig config) {
		this.file = config.getFile();
		this.version = config.getVersion();
		this.reload = config.getReload();

		//System.out.println("real file=" + this.file + " version=" + this.version + " reload=" + this.reload);
	}
}
