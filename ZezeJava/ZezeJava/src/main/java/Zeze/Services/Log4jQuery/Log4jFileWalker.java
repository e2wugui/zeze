package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.Objects;
import Zeze.Util.OutInt;
import org.jetbrains.annotations.NotNull;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 用于Log4jFileSession搜索，具有局部状态。
 */
public class Log4jFileWalker {
	// 状态约定：current==null 表示未打开任何文件（初始/reset后/close后/文件列表为空），此时 currentIndex 恒为0；
	// current!=null 时 current 是 files.get(currentIndex) 的已打开会话，currentIndex==files.size() 表示遍历耗尽。
	// 文件列表会动态增长（onFileCreated），hasNext() 对未打开状态按需打开第一个文件。
	private final @NotNull Log4jFileManager files;
	private int currentIndex;
	private Log4jFileSession current;

	public Log4jFileWalker(@NotNull Log4jFileManager files) {
		Objects.requireNonNull(files);
		this.files = files;
	}

	public void reset() throws IOException {
		if (current != null && currentIndex == 0) {
			current.reset(); // 已持有第一个文件的会话，复用。
			return;
		}
		closeCurrent();
		currentIndex = 0; // 空列表时hasNext()的循环条件(0<size)不成立，保持无会话。
	}

	public void seek(long time) throws IOException {
		var out = new OutInt();
		var log4jFileSession = files.seek(time, out);
		if (log4jFileSession == null) {
			slowSeek(time);
			return;
		}

		currentIndex = out.value;
		closeCurrent();
		current = log4jFileSession;
	}

	private void slowSeek(long time) throws IOException {
		while (hasNext() && current.current().getTime() < time)
			next();
	}

	public boolean hasNext() throws IOException {
		// 循环写法，可以跳过空文件。
		while (currentIndex < files.size()) {
			if (current == null)
				// reset后尚未打开第一个文件，或空列表期间文件被创建（onFileCreated）；currentIndex已在[0,size)内。
				nextCurrent();
			if (current.hasNext())
				return true;
			if (++currentIndex < files.size())
				nextCurrent();
		}
		return false;
	}

	public Log4jLog next() throws IOException {
		return current.next();
	}

	private void nextCurrent() throws IOException {
		closeCurrent();
		current = files.get(currentIndex);
	}

	private void closeCurrent() throws IOException {
		if (current != null) {
			current.close();
			current = null;
		}
	}

	public void close() throws IOException {
		// 关闭最后一个打开的日志文件句柄。
		closeCurrent();
		currentIndex = 0;
	}
}
