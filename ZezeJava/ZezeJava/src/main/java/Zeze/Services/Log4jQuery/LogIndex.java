package Zeze.Services.Log4jQuery;

import java.io.FileOutputStream;

/**
 * 日志按时间顺序的索引。
 * 用来根据时间快速定位到日志数据文件。
 * 每个索引记录固定长度=time(8bytes)+offset(8bytes)。
 *
 * 【扩展】如果索引记录可变长并可以自定义，这个类用途会更加广泛。
 * 变长的实现方式：1. 限制最长记录长度，按最长存储（变成定长）；2. 记录边界可识别（如文本加回车）。
 * 扩展需要实现的话，在新的类中实现，这里仅仅实现Log4jQuery需要的特性。
 */
public class LogIndex {
	public final static int eIndexRecordSize = 16;
	public LogIndex(String fileName) throws Exception {
		// 修正由于文件系统刷新不是原子导致的索引记录可能不完整的问题。
		try (var channel = new FileOutputStream(fileName, true).getChannel()) {
			var fileSize = channel.size();
			if ((fileSize & (eIndexRecordSize - 1)) != 0)
				channel.truncate(fileSize / eIndexRecordSize * eIndexRecordSize);
		}
	}

	public void addIndex(long time, long offset) {

	}

	public long lowerBound(long time) {
		return 0;
	}
}
