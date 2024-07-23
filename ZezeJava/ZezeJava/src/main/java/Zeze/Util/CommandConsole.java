package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import Zeze.Net.AsyncSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandConsole {
	private @NotNull String buffer = "";
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
		input(sender, new String(bytes, StandardCharsets.UTF_8));
	}

	public void input(@NotNull AsyncSocket sender, byte @NotNull [] bytes, int offset, int size) {
		input(sender, new String(bytes, offset, size, StandardCharsets.UTF_8));
	}

	public void input(@NotNull AsyncSocket sender, @NotNull String str) {
		buffer += str;
		tryParseLine(sender);
	}

	public void tryParseLine(@NotNull AsyncSocket sender) {
		for (var lineEnd = buffer.indexOf('\n'); lineEnd >= 0; lineEnd = buffer.indexOf('\n')) {
			var line = buffer.substring(0, lineEnd);
			var words = parseWords(line);
			buffer = buffer.substring(lineEnd + 1);

			// run command
			if (!words.isEmpty()) {
				var cmd = commands.get(words.get(0));
				try {
					cmd.run(sender, words.subList(1, words.size()));
				} catch (Throwable ex) { // print stacktrace.
					sender.Send(Str.stacktrace(ex));
					sender.Send("\r\n" + line + "\r\n");
				}
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
