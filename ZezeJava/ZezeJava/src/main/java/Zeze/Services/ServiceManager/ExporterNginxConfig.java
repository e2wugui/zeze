package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import Zeze.Util.AtomicOutputFile;
import org.jspecify.annotations.Nullable;

/**
 * Nginx配置文件导出器：把SM服务地址写入nginx upstream块，未命中块时追加，可执行reload命令。
 */
public class ExporterNginxConfig implements IExporter {
	private static final org.apache.logging.log4j.Logger logger =
			org.apache.logging.log4j.LogManager.getLogger(ExporterNginxConfig.class);

	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
		// 无可导出地址时跳过，避免生成空 upstream 块导致 nginx reload 失败。
		if (!hasAnyExportableServer(all)) {
			logger.info("ExporterNginxConfig: service '{}' has no identities to export, skip this round", serviceName);
			return;
		}
		var found = false;
		Path path = Path.of(file);
		var source = Files.readString(path, StandardCharsets.UTF_8);
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
			found = true;
		}
		rewritten.append(source, copied, source.length());
		if (!found) {
			if (!rewritten.isEmpty() && rewritten.charAt(rewritten.length() - 1) != '\n')
				rewritten.append('\n');
			var appended = new ArrayList<String>();
			exportToLines("", appended, serviceName, all);
			rewritten.append(String.join("\n", appended)).append('\n');
			logger.info("ExporterNginxConfig: upstream block for '{}' not found, append to end of {}",
					serviceName, file);
		}
		// 原子换版；move失败由failedServices补偿重试。
		AtomicOutputFile.writeAtomically(path, rewritten.toString().getBytes(StandardCharsets.UTF_8));
		reload();
	}

	private record Token(int start, int end, String text, boolean quoted) {
	}

	/** 仅词法扫描：引号、转义和注释中的大括号不参与块边界。 */
	private static @Nullable Token nextToken(String text, int offset) throws IOException {
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
	private final @Nullable String reload;

	private void reload() throws IOException {
		if (null == reload || reload.isBlank())
			return;

		// 丢弃子进程输出并设超时，避免管道满或命令挂起阻塞导出 worker；
		// 显式按空白拆分以兼容带参命令（如 "nginx -s reload"）。
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
	public ExporterNginxConfig(ExporterConfig config) {
		this.file = config.getFile();
		this.version = config.getVersion();
		this.reload = config.getReload();
	}
}
