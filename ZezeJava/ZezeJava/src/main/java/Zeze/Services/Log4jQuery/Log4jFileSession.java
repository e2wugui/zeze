package Zeze.Services.Log4jQuery;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import Zeze.Util.BufferedRandomFile;

/**
 * 单个日志文件的顺序读取会话：按行解析日志（多行续行聚合），支持按索引与时间定位。
 */
public class Log4jFileSession implements Closeable {
	private final File file;
	private final BufferedRandomFile randomAccessFile;
	private Log4jLog nextLog; // 下一条完整的日志。
	private Log4jLog nextNextMaybePartLog; // 下下一条日志，可能不完整。
	private final LogIndex index;
	private final String logTimeFormat;

	@Override
	public String toString() {
		return file.toString();
	}

	public Log4jFileSession(File file, LogIndex index, String charsetName, String logTimeFormat) throws IOException {
		this.file = file;
		this.index = index;
		this.randomAccessFile = new BufferedRandomFile(file, charsetName);
		this.logTimeFormat = logTimeFormat;
		try {
			this.nextLog = tryNext();
		} catch (IOException e) {
			// RAF已打开而构造上抛（如tryNext内readLine的IO错误）：必须先关闭，否则fd只能等GC兜底回收
			// （调用方只捕获FileNotFoundException，构造失败拿不到引用无处close）。close失败不掩盖原始异常。
			try {
				randomAccessFile.close();
			} catch (IOException closeEx) {
				e.addSuppressed(closeEx);
			}
			throw e;
		}
	}

	public File getFile() {
		return file;
	}

	public void reset() throws IOException {
		this.nextNextMaybePartLog = null; // 残留stash属于旧游标位置，不清会被tryNext当作第一条返回（乱序/重复/丢续行）。
		this.randomAccessFile.seek(0);
		this.nextLog = tryNext();
	}

	/**
	 * 定位到 log.time ≥ time 的日志的位置。
	 * 这里只查找一个文件。
	 *
	 * @param time seek time
	 * @return true if seek success
	 */
	public boolean seek(long time) throws IOException {
		return seek(getIndexOffset(time), time);
	}

	public boolean seek(long offset, long time) throws IOException {
		if (offset >= 0) {
			randomAccessFile.seek(offset);
			// 残留stash属于旧游标位置（构造预读/上一轮迭代），不清会被tryNext当作第一条返回——
			// offset落在stash位置之前时迭代乱序+重复（与reset()的stash清理同构；FND24审视波锁定：
			// manager.seek每次"新构造session+立即seek(索引offset)"，查文件头窗口必踩此形态）。
			this.nextNextMaybePartLog = null;
			this.nextLog = tryNext();
			return detailSeek(time);
		}
		return false;
	}

	private long getIndexOffset(long time) {
		// 没有索引时，从头开始搜索。
		if (null != index) {
			// 定位锚=前驱（floor）记录，不是lowerBound（首条>=time的记录）：索引是采样而非
			// 完备集——10s节拍与装载批间基线推进（loadIndex攒满100条才推进lastIndexTime）都
			// 留未索引间隙，lowerBound锚从首条>=time的记录起读，前驱与锚之间时间>=time的日志
			// 物理位置在锚之前，detailSeek只向前推进，永不被读——窗口头部静默漏读（1行/秒
			// 日志批边界后9条丢失形态；水位续扫的beginTime=已投递日志时间而非记录时间，几乎
			// 必落间隙，同漏）。前驱锚从其数据位置起线性读、按time谓词推进，最坏多扫一个
			// 采样节拍段（毫秒级）。floorOffset统一承载边界：time>endTime的前驱即末记录
			// （尾窗查询从末记录续扫不整读，不回落0）；空索引/time早于首条记录返回-1回落0
			// ——首条记录之前仍可有窗口内日志，文件头扫描本就正确。
			var offset = index.floorOffset(time);
			if (offset != -1)
				return offset;
		}
		return 0;
	}

	private boolean detailSeek(long time) throws IOException {
		while (nextLog != null) {
			// detailSeek没有处理当前数据，是定位，所以需要先判断当前数据，之后才next。
			if (nextLog.getTime() >= time)
				return true;
			nextLog = tryNext();
		}
		return false;
	}

	public boolean hasNext() throws IOException {
		return nextLog != null;
	}

	public Log4jLog current() {
		return nextLog;
	}

	public Log4jLog next() throws IOException {
		var next = nextLog;
		nextLog = tryNext();
		return next;
	}

	private Log4jLog tryNextPart() throws IOException {
		String line;
		while (true) {
			var offset = randomAccessFile.getPosition();
			line = randomAccessFile.readLine();
			if (line == null)
				break;
			var log = Log4jLog.tryParse(offset, line, logTimeFormat);
			if (null == log) {
				// 不是日志起始，那么这个肯定是多行日志的孤儿，忽略。
				continue;
			}
			return log;
		}
		return null;
	}

	private Log4jLog tryNext() throws IOException {
		if (null == nextNextMaybePartLog) {
			// 第一次执行或者到达文件结尾。
			nextNextMaybePartLog = tryNextPart();
		}
		// 到达文件结尾。
		if (nextNextMaybePartLog == null)
			return null;

		var next = nextNextMaybePartLog;
		nextNextMaybePartLog = null; // 先清空，下面的循环如果发现有剩下的日志，会重新设置上。
		String line;
		while (true) {
			var offset = randomAccessFile.getPosition();
			line = randomAccessFile.readLine();
			if (line == null)
				break;
			var log = Log4jLog.tryParse(offset, line, logTimeFormat);
			if (null == log) {
				next.addLine(line);
				continue;
			}
			// 下一次将要解析日志，可能不完整。
			nextNextMaybePartLog = log;
			break;
		}
		return next;
	}

	@Override
	public void close() throws IOException {
		randomAccessFile.close();
	}
}
