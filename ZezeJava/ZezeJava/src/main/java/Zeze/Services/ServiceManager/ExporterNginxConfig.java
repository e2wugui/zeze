package Zeze.Services.ServiceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
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
		// FND6-30补：服务暂无任何可导出地址（无identity或passiveIp全空）时整体跳过、不产出
		// 配置——空upstream块会让nginx reload报[emerg] no servers are inside upstream，整个
		// reload失败波及同文件其他服务的地址更新（且reload不查退出码时完全静默）。追加路径
		// 跳过不破坏自愈（有地址后的下一轮照常追加）；已存在块路径保持原块不动（下线地址由
		// nginx自行502，实例重新上线后恢复重写）。
		if (!hasAnyExportableServer(all)) {
			logger.info("ExporterNginxConfig: service '{}' has no identities to export, skip this round", serviceName);
			return;
		}
		var lines = new ArrayList<String>();
		var hasChanged = false;
		var found = false;
		try (var config = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			String line;
			var firstLine = true;
			var skipUntilUpstreamEnd = false;
			while ((line = config.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					// FND6-30补：UTF-8 BOM文件首行同名upstream块不被识别（\uFEFF前缀使startsWith
					// 失配）→ 误判未命中在文件尾追加重复块，nginx报upstream duplicate emerg。
					if (!line.isEmpty() && line.charAt(0) == '\uFEFF')
						line = line.substring(1);
				}
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
		// 改为文件尾追加自产格式块，后续轮转可正常识别重写（空块态由入口的
		// hasAnyExportableServer守卫跳过，不再产出）。
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
			writeConfigAtomic(sb.toString());
			reload();
		}
	}

	// FND8-71：重写必须原子——原Files.writeString(TRUNCATE_EXISTING)截断式覆写，写中途
	// 崩溃/宕机/磁盘满时唯一真源停留在半截状态（nginx重启即拒绝启动、手写内容不可再生，
	// 后续轮次读残缺文件也无法自愈）。同目录临时文件+原子move（复刻GenModule.writeGeneratedFile
	// 惯例，FND7-33），另加move前fsync把断电窗口从页缓存秒级压到纳秒级。他进程占用目标等
	// move失败由Exporter.onEdit的failedServices补偿在下一事件重试。
	private void writeConfigAtomic(String content) throws IOException {
		var targetFile = new File(file);
		var tmp = File.createTempFile(targetFile.getName(), ".tmp", targetFile.getParentFile());
		try {
			try (var fos = new FileOutputStream(tmp);
				 var channel = fos.getChannel()) {
				channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
				channel.force(true); // fsync：防"rename了未落盘数据"的掉电窗口
			}
			var target = targetFile.toPath();
			try {
				Files.move(tmp.toPath(), target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			//noinspection ResultOfMethodCallIgnored
			tmp.delete(); // move成功时tmp已不存在；失败时清理半截临时文件
		}
	}

	private static String trimSuffixBrace(String token) {
		return token.endsWith("{") ? token.substring(0, token.length() - 1) : token;
	}

	/** 服务是否有任何可导出地址（identity存在且passiveIp非空）。 */
	private boolean hasAnyExportableServer(BServiceInfosVersion all) {
		var ver0 = all.getInfos(version);
		if (ver0 == null)
			return false;
		for (var info : ver0.getSortedIdentities())
			if (!info.getPassiveIp().isBlank())
				return true;
		return false;
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

	private void reload() throws IOException {
		if (null == reload || reload.isBlank())
			return;

		// FND6-30补：reload失败不再静默——不查退出码时配置错误（如nginx -t不过）无任何
		// 可见性，波及同文件其他服务的地址更新。
		// 本方法运行在Exporter.onEdit的one-by-one单worker：输出DISCARD丢弃（无人读管道时
		// 输出填满OS缓冲会让命令自身死锁）、有界等待+超时强杀（命令挂起不得冻结worker，
		// 否则所有后续SM事件停摆、failedServices补偿同worker永不运行）。
		// Runtime.exec(String)按空白切分，ProcessBuilder不切——显式split保持带参命令
		// （如"nginx -s reload"）语义不变。
		var p = new ProcessBuilder(reload.split("\\s+"))
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();
		try {
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				logger.error("ExporterNginxConfig: reload command '{}' timed out after 30s, forcibly killed", reload);
				return;
			}
			var exit = p.exitValue();
			if (exit != 0)
				logger.error("ExporterNginxConfig: reload command '{}' exited with {}, config may be invalid",
						reload, exit);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			p.destroyForcibly();
		}
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
