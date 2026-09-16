package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import Zeze.Net.AsyncSocket;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandConsole {
	// FND7-50：控制台行协议按'\n'分帧，未终结行的行缓冲必须有上界（默认64K，可用系统属性
	// commandConsoleMaxLineSize调整）。命令行控制台无鉴权，Acceptor默认全网卡监听，
	// 任意客户端发送无换行字节流即可把无界的行缓冲当累积点耗尽进程堆。
	public static final int MAX_LINE_BUFFER_SIZE = Str.parseIntSize(System.getProperty("commandConsoleMaxLineSize"), 64 * 1024);

	// FND7-51：行缓冲按字节累积（非String）：读块边界可能切在多字节UTF-8字符中间，
	// 按块独立解码会产出U+FFFD替换字符——仅在完整行边界做一次UTF-8解码。
	private final @NotNull ByteBuffer buffer = ByteBuffer.Allocate(128);
	private final HashMap<String, Command> commands = new HashMap<>();

	static @NotNull CommandConsole dup(@NotNull CommandConsole cc) {
		var dup = new CommandConsole();
		dup.commands.putAll(cc.commands);
		return dup;
	}

	@FunctionalInterface
	public interface Command {
		void run(@NotNull AsyncSocket sender, @NotNull List<String> arguments);
	}

	public static class Options {
		public final LinkedHashMap<String, String> properties = new LinkedHashMap<>();
		public final List<String> others = new ArrayList<>();

		public @Nullable String property(@NotNull String name) {
			return properties.get(name);
		}

		public @NotNull List<String> others() {
			return others;
		}

		public boolean contains(@NotNull String name) {
			return properties.containsKey(name);
		}

		private void buildProperty(@NotNull String property) {
			var i = property.indexOf('=');
			if (i >= 0)
				properties.put(property.substring(0, i), property.substring(i + 1));
			else
				properties.put(property, null);
		}

		public void buildJvm(@NotNull List<String> args) {
			for (String arg : args) {
				if (arg.startsWith("-D"))
					buildProperty(arg.substring(2));
				else
					others.add(arg);
			}
		}

		@Override
		public @NotNull String toString() {
			return properties.toString() + others;
		}

		public static @NotNull Options parseJvm(@NotNull List<String> args) {
			var options = new Options();
			options.buildJvm(args);
			return options;
		}

		public static @NotNull Options parseProperty(@NotNull List<String> args) {
			var options = new Options();
			for (var arg : args)
				options.buildProperty(arg);
			return options;
		}
	}

	public void register(@NotNull String name, @NotNull Command cmd) {
		if (commands.putIfAbsent(name, cmd) != null)
			throw new IllegalStateException("duplicate command: " + name);
	}

	public void input(@NotNull AsyncSocket sender, byte @NotNull [] bytes) {
		input(sender, bytes, 0, bytes.length);
	}

	public void input(@NotNull AsyncSocket sender, byte @NotNull [] bytes, int offset, int size) {
		buffer.Append(bytes, offset, size);
		tryParseLine(sender);
		checkLineBufferLimit();
	}

	public void input(@NotNull AsyncSocket sender, @NotNull String str) {
		// R3-U2（A/FND7-51披露）：String直驱路径经UTF-8编码进入字节行缓冲（与socket读块同
		// 路径，受同一上限约束）。行为变化：孤立代理项字符（unpaired surrogate）被编码器
		// 替换为'?'——原String拼接会原样保留；合法BMP/增补字符（含emoji）不变。
		input(sender, str.getBytes(StandardCharsets.UTF_8));
	}

	// FND7-50：检查消费完整行之后的残留（未终结行）。量纲与TcpSocket.processReceive的
	// remain检查一致（CommandConsoleService总是整块消费使后者永不触发，防线移到这里）；
	// 瞬态上界=上限+单读块大小。超限抛错沿OnSocketProcessInputBuffer→processReceive→
	// doException→close关闭连接。
	private void checkLineBufferLimit() {
		if (buffer.size() > MAX_LINE_BUFFER_SIZE) {
			var len = buffer.size();
			buffer.Reset(); // 抛错前清空：捕获异常继续使用的调用方（进程内驱动sender==null）不至于永久饱和
			throw new IllegalStateException("CommandConsole line buffer overflow: " + len
					+ " > " + MAX_LINE_BUFFER_SIZE + " (unterminated line?)");
		}
	}

	public void tryParseLine(@NotNull AsyncSocket sender) {
		for (var lineEnd = indexOfNewline(); lineEnd >= 0; lineEnd = indexOfNewline()) {
			var line = new String(buffer.Bytes, buffer.ReadIndex, lineEnd - buffer.ReadIndex, StandardCharsets.UTF_8);
			buffer.ReadIndex = lineEnd + 1; // remove the consumed line before parsing,
			// otherwise a malformed line (unclosed quote) will keep throwing and poison the buffer permanently

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

	private void runLine(@NotNull AsyncSocket sender, @NotNull String line) {
		ArrayList<String> words;
		try {
			words = parseWords(line);
		} catch (IllegalStateException ex) { // unclosed quote
			//noinspection ConstantValue
			if (sender != null)
				sender.Send("error command format: " + line + "\r\n");
			return;
		}

		// run command
		if (words.isEmpty())
			return;
		var cmd = commands.get(words.getFirst());
		if (cmd == null) {
			//noinspection ConstantValue
			if (sender != null) // sender 可为 null（如类内 main 以 cc.input(null, ...) 驱动）
				sender.Send("unknown command: " + words.getFirst() + "\r\n");
			return;
		}
		try {
			cmd.run(sender, words.subList(1, words.size()));
		} catch (Throwable ex) { // print stacktrace.
			//noinspection ConstantValue
			if (sender != null) { // 同上：与上面的判空保持一致
				sender.Send(Str.stacktrace(ex));
				sender.Send("\r\n" + line + "\r\n");
			}
		}
	}

	public static @NotNull ArrayList<String> parseWords(@NotNull String line) {
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

	private static void dump(@NotNull AsyncSocket sender, @NotNull List<String> args) {
		System.out.println(args);
		System.out.println(Options.parseJvm(args));
		System.out.println(Options.parseProperty(args));
	}

	public static void main(String @NotNull [] args) {
		var cc = new CommandConsole();
		cc.register("a", CommandConsole::dump);
		cc.register("2", CommandConsole::dump);
		cc.register("3", CommandConsole::dump);

		//noinspection DataFlowIssue
		cc.input(null, "a -Dn1=v -D\"n3=v v\" d -Dn2=\"v v\" \"x x\"\n");
		//cc.input(null, "2  xx  -b\t-c cc\n3 -4\n");
	}
}
