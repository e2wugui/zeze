package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import Zeze.Util.AtomicFileWriter;

/**
 * Nginx配置文件导出器：把SM服务地址写入nginx upstream块，未命中块时追加，可执行reload命令。
 */
public class ExporterNginxConfig implements IExporter {
	private static final @NotNull org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(ExporterNginxConfig.class);

	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
		// 服务暂无任何可导出地址（无identity或passiveIp全空）时整体跳过、不产出
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
		var source = Files.readString(Path.of(file), StandardCharsets.UTF_8);
		if (source.startsWith("\uFEFF"))
			source = source.substring(1);
		var rewritten = new StringBuilder();
		int copied = 0;
		for (var token = nextToken(source, 0); token != null; token = nextToken(source, token.end)) {
			if (token.quoted || !token.text.equals("upstream"))
				continue;
			var name = nextToken(source, token.end);
			if (name == null || !name.text.equals(serviceName))
				continue;
			var brace = nextToken(source, name.end);
			if (brace == null || brace.quoted || !brace.text.equals("{"))
				throw new IOException("missing upstream opening brace: " + serviceName);
			int depth = 1;
			Token close = brace;
			while (depth != 0) {
				close = nextToken(source, close.end);
				if (close == null)
					throw new IOException("unterminated upstream: " + serviceName);
				if (!close.quoted && close.text.equals("{"))
					++depth;
				else if (!close.quoted && close.text.equals("}"))
					--depth;
			}
			int lineStart = source.lastIndexOf('\n', token.start) + 1;
			var prefix = source.substring(lineStart, token.start);
			int replaceStart = prefix.isBlank() ? lineStart : token.start;
			if (!prefix.isBlank())
				prefix = "";
			rewritten.append(source, copied, replaceStart);
			var replacement = new ArrayList<String>();
			exportToLines(prefix, replacement, serviceName, all);
			rewritten.append(String.join("\n", replacement));
			copied = close.end;
			token = close;
			found = hasChanged = true;
		}
		rewritten.append(source, copied, source.length());
		if (found)
			lines.add(rewritten.toString());
		else
			lines.add(source);

		// 整文件未命中同名upstream块时恒不更新（hasChanged恒false）——新增服务
		// 或首次部署未预置空块时该服务地址永不进nginx、无自愈无告警（未文档化的隐含契约）。
		// 文件尾追加自产格式块，后续轮转可正常识别重写（空块态由入口的
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
			// 原子换版；move失败由failedServices补偿重试。
			AtomicFileWriter.replace(Path.of(file), sb.toString().getBytes(StandardCharsets.UTF_8));
			reload();
		}
	}

	private record Token(int start, int end, String text, boolean quoted) {
	}

	/** 仅词法扫描：引号、转义和注释中的大括号不参与块边界。 */
	private static Token nextToken(String text, int offset) throws IOException {
		int length = text.length();
		while (offset < length) {
			char ch = text.charAt(offset);
			if (Character.isWhitespace(ch)) {
				++offset;
				continue;
			}
			if (ch == '#') {
				int newline = text.indexOf('\n', offset);
				offset = newline >= 0 ? newline + 1 : length;
				continue;
			}
			break;
		}
		if (offset == length)
			return null;
		int start = offset;
		char first = text.charAt(offset++);
		if (first == '{' || first == '}' || first == ';')
			return new Token(start, offset, String.valueOf(first), false);
		var value = new StringBuilder();
		char quote = first == '\'' || first == '"' ? first : 0;
		boolean escaped = first == '\\' && offset < length;
		if (quote == 0) {
			if (escaped)
				value.append(text.charAt(offset++));
			else
				value.append(first);
		}
		while (offset < length) {
			char ch = text.charAt(offset);
			if (ch == '\\' && offset + 1 < length) {
				escaped = true;
				value.append(text.charAt(offset + 1));
				offset += 2;
			} else if (quote != 0) {
				++offset;
				if (ch == quote)
					return new Token(start, offset, value.toString(), true);
				value.append(ch);
			} else if (Character.isWhitespace(ch) || ch == '{' || ch == '}' || ch == ';' || ch == '#')
				break;
			else {
				value.append(ch);
				++offset;
			}
		}
		if (quote != 0)
			throw new IOException("unterminated nginx quoted token at " + start);
		return new Token(start, offset, value.toString(), escaped);
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

		// reload失败不静默——不查退出码时配置错误（如nginx -t不过）无任何
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
				throw new IOException("nginx reload timed out after 30s: " + reload);
			}
			var exit = p.exitValue();
			if (exit != 0)
				throw new IOException("nginx reload exited with " + exit + ": " + reload);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			p.destroyForcibly();
			throw new IOException("nginx reload interrupted: " + reload, e);
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
	}
}
