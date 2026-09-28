package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.Objects;
import Zeze.Util.OutInt;
import Zeze.Util.OutObject;
import org.jetbrains.annotations.NotNull;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 用于Log4jFileSession搜索，具有局部状态。
 */
public class Log4jFileWalker {
	// 状态约定：current==null 表示未打开任何文件（初始/reset后/close后/文件列表为空），此时 currentIndex 恒为0；
	// current!=null 时 current 是从条目 currentEntry 打开的会话，currentIndex 为该条目在 files 中的当前位置
	//（每次hasNext入口按引用重同步），currentIndex==files.size() 表示遍历耗尽。
	// 文件列表动态增长（onFileCreated）且可被并发摘除（removeMissingFile/reconcile）：
	// CopyOnWriteArrayList摘除使其后元素左移，整型下标单独不可信，currentEntry 引用是重定位锚点。
	private final @NotNull Log4jFileManager files;
	private int currentIndex;
	private Log4jFileSession current;
	private Log4jFileManager.Log4jFile currentEntry;
	// close终态墓碑：先置位再动作（对齐Session.closed惯例）。
	// 惰性清理与并发查询的取锁竞速只防"关的时候正在查"，不防"关完了才查"——
	// 无终态时迟到的hasNext会重开files.get(0)复活会话（句柄泄漏+从头重扫返回错窗数据）。
	private boolean closed;

	public Log4jFileWalker(@NotNull Log4jFileManager files) {
		Objects.requireNonNull(files);
		this.files = files;
	}

	public void reset() throws IOException {
		// 复用判定按条目引用定位：摘除左移后"currentIndex==0"可能指向别的条目或已摘除的当前条目
		//（僵尸会话从头重扫，Linux下打开中文件可被unlink）。indexOf==-1自然落入关闭分支。
		if (current != null && files.indexOf(currentEntry) == 0) {
			current.reset(); // 已持有第一个文件的会话，复用。
			return;
		}
		closeCurrent();
		currentIndex = 0; // 空列表时hasNext()的循环条件(0<size)不成立，保持无会话。
	}

	public void seek(long time) throws IOException {
		if (closed)
			throw new IllegalStateException("walker closed"); // close后不得复活
		var out = new OutInt();
		var outEntry = new OutObject<Log4jFileManager.Log4jFile>();
		var log4jFileSession = files.seek(time, out, outEntry);
		if (log4jFileSession == null) {
			slowSeek(time);
			return;
		}

		// 先接管新会话再关旧会话：closeCurrent对旧会话的close抛IOException时，
		// manager返回的新会话已由current持有，随后的close()/closeCurrent()统一释放，不泄漏。
		var stale = current;
		currentIndex = out.value;
		current = log4jFileSession;
		currentEntry = outEntry.value; // 与会话同源捕获，下标仅供循环条件参考
		if (stale != null)
			stale.close();
	}

	private void slowSeek(long time) throws IOException {
		while (hasNext() && current.current().getTime() < time)
			next();
	}

	public boolean hasNext() throws IOException {
		if (closed)
			throw new IllegalStateException("walker closed"); // close后不得复活（重开文件/从头重扫）
		// 引用重同步：持有会话期间条目可被并发摘除（removeMissingFile/reconcile），
		// COW摘除使其后元素左移、下标失真（++会跳过整文件或提前终止），先按indexOf校正再使用。
		// 条目已摘除（pos<0）时current仍可读尽（Linux unlink后fd有效，读尽不丢当前文件尾部），
		// 耗尽后由advanceCurrent按clamp接时间序后继。
		if (current != null && currentEntry != null) {
			var pos = files.indexOf(currentEntry);
			if (pos >= 0)
				currentIndex = pos;
			else if (current.hasNext())
				return true; // 条目已摘除（Linux unlink形态，pos<0）：会话fd仍可读，读尽不丢当前文件尾部——
				// 此时stale下标可能>=size，while闸先判假会把未读尾部拦在循环体外。
		}
		// 循环写法，可以跳过空文件；文件被外部清理的条目由manager.get持锁摘除后继续。
		while (currentIndex < files.size()) {
			if (current == null)
				// reset后尚未打开第一个文件，或空列表期间文件被创建（onFileCreated）；currentIndex已在[0,size)内。
				nextCurrent();
			if (current == null)
				return false; // 残余条目全部打不开（已被摘除，currentIndex>=files.size()），遍历耗尽。
			if (current.hasNext())
				return true;
			advanceCurrent();
		}
		return false;
	}

	public Log4jLog next() throws IOException {
		if (closed)
			throw new IllegalStateException("walker closed");
		return current.next();
	}

	/**
	 * current耗尽，定位到其时间序后继：
	 * 条目仍在列表——后继=pos+1，按引用计算，列表增长/收缩（左移）都不再依赖整型下标自增；
	 * 条目已摘除——其后继已左移到当前下标处，按currentIndex clamp（不++），单次摘除数学上恰得后继
	 * （读IO期间并发多次摘除的残余错位为已知限制）。
	 */
	private void advanceCurrent() throws IOException {
		var pos = files.indexOf(currentEntry);
		currentIndex = pos >= 0 ? pos + 1 : Math.min(currentIndex, files.size());
		// 后继条目引用必须在closeCurrent之前捕获（log4j-01）：close的RAF IO（毫秒级）期间
		// currentEntry已置null、hasNext入口的引用重同步失效，窗口内COW摘除（reconcile/
		// removeMissingFile）左移会使定格下标错位——按定格下标get开出原后继的后继（整文件
		// 静默跳过）或越界。按引用开文件后窗口内的摘除不再影响目标：引用即后继；恰被摘除
		// 则FNFE走清理收尾（耗尽或hasNext循环重开），单次摘除数学与indexOf求值之前的形态一致。
		var next = files.entryAt(currentIndex);
		closeCurrent();
		if (next != null) {
			current = files.open(next);
			currentEntry = current != null ? next : null;
			// currentIndex维持定格值即可：currentEntry非空时hasNext入口按引用重同步即时校正；
			// open失败（null）时hasNext循环按该下标走既有get重试路径（含FNFE摘除重试形态）。
		}
	}

	private void nextCurrent() throws IOException {
		closeCurrent();
		// get对被外部清理的文件摘除条目后继续（同index重试形态保留），残余条目全部打不开时
		// 返回null由hasNext收尾；outEntry同步回传实际打开的条目，与下标组成双锚点。
		var outEntry = new OutObject<Log4jFileManager.Log4jFile>();
		current = files.get(currentIndex, outEntry);
		currentEntry = current != null ? outEntry.value : null;
	}

	private void closeCurrent() throws IOException {
		var stale = current;
		if (stale != null) {
			// 先复位再关闭（与seek路径"先接管新会话再关旧会话"同构的隔离）：close抛IOException时
			// 引用不残留——残留的已关/半关会话会让后续hasNext/next对死会话连锁抛ClosedChannelException
			// 等二次异常（每次翻页重放）；复位后advance/next路径按列表重开文件（响亮失败至多一次）。
			current = null;
			currentEntry = null;
			stale.close();
		}
	}

	public void close() throws IOException {
		closed = true; // 先立墓碑（对齐Session.closed）：close后的hasNext/next/seek快速失败
		closeCurrent();
		currentIndex = 0;
	}
}
