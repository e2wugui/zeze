package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/** 命令行控制台：按 '\n' 分帧的行协议解析、引号分词与命令注册分发。 */
public class CommandConsole {
	private static final Logger logger = LogManager.getLogger(CommandConsole.class);

	// 未终结行缓冲上限（默认 64K，-DcommandConsoleMaxLineSize 可配），防止无换行流耗尽堆内存。
	public static final int MAX_LINE_BUFFER_SIZE =
			resolveMaxLineBufferSize(System.getProperty("commandConsoleMaxLineSize"), 64 * 1024);

	// 按字节累积并在完整行边界做一次 UTF-8 解码，避免读块切分多字节字符。
	private final ByteBuffer buffer = ByteBuffer.Allocate(128);
	private final HashMap<String, Command> commands = new HashMap<>();

	static CommandConsole dup(CommandConsole cc) {
		var dup = new CommandConsole();
		dup.commands.putAll(cc.commands);
		return dup;
	}

	@FunctionalInterface
	public interface Command {
		void run(@Nullable AsyncSocket sender, List<String> arguments);
	}

	public static class Options {
		public final LinkedHashMap<String, @Nullable String> properties = new LinkedHashMap<>();
		public final List<String> others = new ArrayList<>();

		public @Nullable String property(String name) {
			return properties.get(name);
		}

		public List<String> others() {
			return others;
		}

		public boolean contains(String name) {
			return properties.containsKey(name);
		}

		private void buildProperty(String property) {
			var i = property.indexOf('=');
			if (i >= 0)
				properties.put(property.substring(0, i), property.substring(i + 1));
			else
				properties.put(property, null);
		}

		public void buildJvm(List<String> args) {
			for (String arg : args) {
				if (arg.startsWith("-D"))
					buildProperty(arg.substring(2));
				else
					others.add(arg);
			}
		}

		@Override
		public String toString() {
			return properties.toString() + others;
		}

		public static Options parseJvm(List<String> args) {
			var options = new Options();
			options.buildJvm(args);
			return options;
		}

		public static Options parseProperty(List<String> args) {
			var options = new Options();
			for (var arg : args)
				options.buildProperty(arg);
			return options;
		}
	}

	// 解析 commandConsoleMaxLineSize；非法或非正值记告警并回落默认值。
	@SuppressWarnings("SameParameterValue")
	static int resolveMaxLineBufferSize(@Nullable String propValue, int defSize) {
		if (propValue == null)
			return defSize;
		int v;
		try {
			v = Str.parseIntSize(propValue, defSize);
		} catch (NumberFormatException | IllegalStateException e) {
			logger.warn("invalid -DcommandConsoleMaxLineSize='{}', fall back to default {}", propValue, defSize, e);
			return defSize;
		}
		if (v <= 0) {
			logger.warn("invalid -DcommandConsoleMaxLineSize='{}' ({} <= 0), fall back to default {}",
					propValue, v, defSize);
			return defSize;
		}
		return v;
	}

	public void register(String name, Command cmd) {
		if (commands.putIfAbsent(name, cmd) != null)
			throw new IllegalStateException("duplicate command: " + name);
	}

	public void input(@Nullable AsyncSocket sender, byte [] bytes) {
		input(sender, bytes, 0, bytes.length);
	}

	public void input(@Nullable AsyncSocket sender, byte [] bytes, int offset, int size) {
		buffer.Append(bytes, offset, size);
		tryParseLine(sender);
		checkLineBufferLimit();
	}

	public void input(@Nullable AsyncSocket sender, String str) {
		input(sender, str.getBytes(StandardCharsets.UTF_8));
	}

	// 检查消费完整行后的未终结残留长度，超限抛错由网络层关闭连接。
	private void checkLineBufferLimit() {
		if (buffer.size() > MAX_LINE_BUFFER_SIZE) {
			var len = buffer.size();
			buffer.Reset(); // 抛错前清空，避免进程内复用方持续溢出
			throw new IllegalStateException("CommandConsole line buffer overflow: " + len
					+ " > " + MAX_LINE_BUFFER_SIZE
					+ " (unterminated line? raise -DcommandConsoleMaxLineSize, e.g. 1M or max)");
		}
	}

	public void tryParseLine(@Nullable AsyncSocket sender) {
		for (var lineEnd = indexOfNewline(); lineEnd >= 0; lineEnd = indexOfNewline()) {
			var line = new String(buffer.Bytes, buffer.ReadIndex, lineEnd - buffer.ReadIndex, StandardCharsets.UTF_8);
			buffer.ReadIndex = lineEnd + 1; // 先推进读游标再解析，避免畸形行永久阻塞缓冲

			runLine(sender, line);
		}
		buffer.Compact();
	}

	private int indexOfNewline() {
		var bytes = buffer.Bytes;
		for (int i = buffer.ReadIndex, end = buffer.WriteIndex; i < end; i++)
			if (bytes[i] == '\n')
				return i;
		return -1;
	}

	private void runLine(@Nullable AsyncSocket sender, String line) {
		ArrayList<String> words;
		try {
			words = parseWords(line);
		} catch (IllegalStateException ex) {
			if (sender != null)
				sender.Send("error command format: " + line + "\r\n");
			return;
		}

		if (words.isEmpty())
			return;
		var cmd = commands.get(words.getFirst());
		if (cmd == null) {
			if (sender != null)
				sender.Send("unknown command: " + words.getFirst() + "\r\n");
			return;
		}
		try {
			cmd.run(sender, words.subList(1, words.size()));
		} catch (Throwable ex) {
			if (sender != null) {
				sender.Send(Str.stacktrace(ex));
				sender.Send("\r\n" + line + "\r\n");
			}
		}
	}

	public static ArrayList<String> parseWords(String line) {
		var quotBegin = -1;
		var wordBegin = 0;
		var words = new ArrayList<String>();
		for (var i = 0; i < line.length(); ++i) {
			var c = line.charAt(i);

			if (c == '"') {
				if (quotBegin == -1)
					quotBegin = i + 1;
				else {
					var w = "";
					if (line.charAt(wordBegin) == '"')
						wordBegin += 1;
					var wordEnd = quotBegin - 1;
					if (wordEnd > wordBegin)
						w = line.substring(wordBegin, wordEnd);
					w += line.substring(quotBegin, i);
					words.add(w);
					quotBegin = -1;
					wordBegin = i + 1;
				}
			} else if (quotBegin == -1) {
				if (Character.isWhitespace(c)) {
					if (i > wordBegin) {
						words.add(line.substring(wordBegin, i));
					}
					wordBegin = i + 1;
				}
			}
		}
		if (quotBegin != -1)
			throw new IllegalStateException("error command format: " + line);

		if (line.length() > wordBegin)
			words.add(line.substring(wordBegin));

		return words;
	}

	private static void dump(@Nullable AsyncSocket sender, List<String> args) {
		System.out.println(args);
		System.out.println(Options.parseJvm(args));
		System.out.println(Options.parseProperty(args));
	}

	public static void main(String [] args) {
		var cc = new CommandConsole();
		cc.register("a", CommandConsole::dump);
		cc.register("2", CommandConsole::dump);
		cc.register("3", CommandConsole::dump);
	}
}
