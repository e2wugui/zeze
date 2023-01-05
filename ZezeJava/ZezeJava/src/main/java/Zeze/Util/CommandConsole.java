package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import Zeze.Net.AsyncSocket;

public class CommandConsole {
	private String buffer = "";
	private HashMap<String, Command> commands = new HashMap<>();

	static CommandConsole dup(CommandConsole cc) {
		var dup = new CommandConsole();
		dup.commands.putAll(cc.commands);
		return dup;
	}

	@FunctionalInterface
	public interface Command {
		public void run(AsyncSocket sender, List<String> arguments);
	}

	public static class Options {
		public HashMap<String, List<String>> options = new HashMap<>();

		public List<String> options(String name) {
			return options.get(name);
		}

		public String option(String name) {
			var ops = options.get(name);
			if (null == ops)
				return null;
			if (ops.size() != 1)
				throw new RuntimeException("options not only one.");
			return ops.get(0);
		}

		public boolean contains(String name) {
			return options.get(name) != null;
		}

		public void build(List<String> args) {
			var opBegin = -1;
			for (int i = 0; i < args.size(); ++i) {
				var arg = args.get(i);
				if (arg.startsWith("-")) {
					if (opBegin != -1) {
						options.putIfAbsent(args.get(opBegin), args.subList(opBegin + 1, i));
						opBegin = i + 1;
					}
					opBegin = i;
				}
			}
			if (opBegin != -1) {
				options.putIfAbsent(args.get(opBegin), args.subList(opBegin + 1, args.size()));
			}
		}

		@Override
		public String toString() {
			return options.toString();
		}

		public static Options parse(List<String> args) {
			var options = new Options();
			options.build(args);
			return options;
		}
	}

	public void register(String name, Command cmd) {
		if (null != commands.putIfAbsent(name, cmd))
			throw new RuntimeException("duplicate command: " + name);
	}

	public void input(AsyncSocket sender, byte[] bytes) {
		input(sender, new String(bytes, StandardCharsets.UTF_8));
	}

	public void input(AsyncSocket sender, byte[] bytes, int offset, int size) {
		input(sender, new String(bytes, offset, size, StandardCharsets.UTF_8));
	}

	public void input(AsyncSocket sender, String str) {
		buffer += str;
		tryParseLine(sender);
	}

	public void tryParseLine(AsyncSocket sender) {
		for (var lineEnd = buffer.indexOf('\n'); lineEnd >= 0; lineEnd = buffer.indexOf('\n')) {
			var line = buffer.substring(0, lineEnd);
			var words = parseWords(line);
			buffer = buffer.substring(lineEnd + 1);

			// run command
			if (!words.isEmpty()) {
				var cmd = commands.get(words.get(0));
				try {
					cmd.run(sender, words.subList(1, words.size()));
				} catch (Throwable ex) {
					sender.Send(Str.stacktrace(ex));
					sender.Send("\r\n" + line + "\r\n");
				}
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
					words.add(line.substring(quotBegin, i));
					quotBegin = -1;
					wordBegin = i + 1;
				}
			} else {
				if (Character.isWhitespace(c)) {
					if (i > wordBegin) {
						words.add(line.substring(wordBegin, i));
					}
					wordBegin = i + 1;
				}
			}
		}
		if (quotBegin != -1)
			throw new RuntimeException("error command format: " + line);

		if (line.length() > wordBegin)
			words.add(line.substring(wordBegin));

		return words;
	}

	private static void dump(AsyncSocket sender, List<String> args) {
		System.out.println(Options.parse(args));
	}

	public static void main(String args[]) {
		var cc = new CommandConsole();
		cc.register("a", CommandConsole::dump);
		cc.register("2", CommandConsole::dump);
		cc.register("3", CommandConsole::dump);

		cc.input(null, "a -b c d -d \"xx\"");
		cc.input(null, "\n");
		cc.input(null, "2  xx  -b\t-c cc\n3 -4\n");
	}
}
