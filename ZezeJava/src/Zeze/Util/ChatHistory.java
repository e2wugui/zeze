package Zeze.Util;

import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

/** 
 *性能性能性能性能
 每条消息顺序存储，严重依赖写文件不能出错。
 *写文件部分失败，导致不能工作。
 *写两个文件，数据和索引不一致会导致不能工作。
 其中索引损坏可以从数据文件重建，
 数据文件顺坏，就涉及消息记录定位问题，目前定位能力比较弱。
*/

public final class ChatHistory implements Closeable {
	private String HistoryHome;
	public String getHistoryHome() {
		return HistoryHome;
	}
	private String SessionHome;
	public String getSessionHome() {
		return SessionHome;
	}
	private String ContentHome;
	public String getContentHome() {
		return ContentHome;
	}
	private long SessionId;
	public long getSessionId() {
		return SessionId;
	}
	private long MaxSingleDataFileLength;
	public long getMaxSingleDataFileLength() {
		return MaxSingleDataFileLength;
	}
	private long SeparateContentLength;
	public long getSeparateContentLength() {
		return SeparateContentLength;
	}
	private long LastId;
	public long getLastId() {
		return LastId;
	}
	private void setLastId(long value) {
		LastId = value;
	}
	private long FirstId;
	public long getFirstId() {
		return FirstId;
	}
	private void setFirstId(long value) {
		FirstId = value;
	}

	public ChatHistory(String historyHome, long sessionId, long maxSingleDataFileLength, long separateContentLength) {
		if (false == (new File(historyHome)).isDirectory()) {
			throw new IllegalArgumentException("history home not exist.");
		}

		this.HistoryHome = historyHome;
		this.SessionHome = Paths.get(historyHome).resolve(String.valueOf(sessionId)).toString();
		this.ContentHome = Paths.get(this.getSessionHome()).resolve("contents").toString();
		(new File(this.getSessionHome())).mkdirs();
		(new File(this.getContentHome())).mkdirs();

		this.SessionId = sessionId;
		this.MaxSingleDataFileLength = maxSingleDataFileLength;
		this.SeparateContentLength = separateContentLength;

		LoadFileStartIdsAndInit();
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] public long AddMessage(string sender, string content)
	public long AddMessage(String sender, String content) {
		return AddMessage(sender, ChatHistoryMessage.TypeString, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	/** 
	 增加消息到聊天历史中。
	 
	 @param sender 发送者
	 @param type 消息类型
	 @param content 消息内容
	 @return 
	*/
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long AddMessage(string sender, int type, byte[] content)
	public long AddMessage(String sender, int type, byte[] content) {
		if (type < 0) {
			throw new IllegalArgumentException("type is reserved: " + type);
		}

		synchronized (this) {
			ChatHistoryMessage msg = new ChatHistoryMessage();
			msg.setTag(0);
			msg.setId(getLastId());
			msg.setTimeTicks(LocalDateTime.now().getTime());
			msg.setSender(sender);
			msg.setType(type);
			msg.setContent(content);

			if (this.getSeparateContentLength() > 0 && content.length > this.getSeparateContentLength()) {
				msg.setTag(ChatHistoryMessage.TagSeparate);
				msg.SaveContentToFile(Paths.get(this.getContentHome()).resolve(String.valueOf(msg.getId())).toString());
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: msg.Content = Array.Empty<byte>();
				msg.setContent(Array.<Byte>Empty()); // 对于图片视频，这里可以考虑放一个缩小的提示性图片。
			}

			if (this.getMaxSingleDataFileLength() > 0 && _lastDataFile.getDataFileLength() > this.getMaxSingleDataFileLength()) {
				OpenOrCreateLastDataFile(getLastId());
			}

			Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Allocate(msg.SizeHint());
			int state;
			tangible.OutObject<Integer> tempOut_state = new tangible.OutObject<Integer>();
			bb.BeginWriteWithSize4(tempOut_state);
		state = tempOut_state.outArgValue;
			msg.Encode(bb);
			bb.EndWriteWithSize4(state);
			_lastDataFile.WriteToTail(bb.getBytes(), bb.getReadIndex(), bb.getSize());

			var tempVar = getLastId(); // 最后才真的增加，避免上面异常导致LastId已被增加。
		setLastId(getLastId() + 1);
		return tempVar;
		}
	}

	/** 
	 读取最近的几条消息。
	 
	 @param count 消息个数，-1 表示读取全部
	 @return 
	*/
	public ArrayList<ChatHistoryMessage> ReadMessageRecent(int count) {
		if (count < 0) { // read all
			return ReadMessage(getFirstId(), -1);
		}

		long fromId = getLastId() - count;
		if (fromId < getFirstId()) {
			fromId = getFirstId();
		}

		return ReadMessage(fromId, count);
	}
	/** 
	 从 指定fromId 开始顺序读取一定数量的消息。
	 
	 @param fromId 开始的Id，包含。如果fromId所在的数据已经被删除，返回空List。外面根据需要可以判断FirstId修正，内部不自动修正。 
	 @param count 读取数量，-1 一直读到结尾。
	 @return 
	*/
	public ArrayList<ChatHistoryMessage> ReadMessage(long fromId, int count) {
		if (fromId < 0) {
			throw new IllegalArgumentException();
		}

		ArrayList<ChatHistoryMessage> result = new ArrayList<ChatHistoryMessage>();

		long countReal = count; // 内部使用long类型
		if (countReal < 0) {
			countReal = getLastId() - fromId;
		}
		if (countReal < 0) {
			return result;
		}

		synchronized (this) {
			int startIdIndex = FindStartIdIndex(fromId);
			if (startIdIndex < 0) {
				return result;
			}

			for (; countReal > 0 && startIdIndex < _fileStartIds.size(); ++startIdIndex) {
				long startId = _fileStartIds.get(startIdIndex);
				if (_lastDataFile.getStartId() == startId) {
					//int realReadCount = 
					_lastDataFile.Read(fromId, countReal, result);
					return result; // 最后一个文件，读多少算多少，直接返回。
				}

				try (MessageFile mf = new MessageFile(this, startId)) {
					long r = mf.Read(fromId, count, result);
					if (r > 0) {
						countReal -= r;
						fromId = result.get(result.size() - 1).getId() + 1;
					}
				}
			}
		}
		return result;
	}

	public void DeleteMessage(long id) {
		int startIdIndex = FindStartIdIndex(id);
		if (startIdIndex < 0) {
			return;
		}

		long startId = _fileStartIds.get(startIdIndex);
		if (_lastDataFile.getStartId() == startId) {
			_lastDataFile.Delete(id);
			return;
		}

		try (MessageFile mf = new MessageFile(this, startId)) {
			mf.Delete(id);
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] LoadContentFromFile(long id)
	public byte[] LoadContentFromFile(long id) {
		return ChatHistoryMessage.LoadContentFromFile(Paths.get(this.getContentHome()).resolve(String.valueOf(id)).toString());
	}

	/** 
	 删除最后写文件的时间在time以前的数据和索引文件。
	 最后一个（当前）文件不会被删除: 因为要记住当前的msgId。全部清除会导致msgId重新从0开始。
	 
	 @param timeTicks
	*/
	public void DeleteFileBefore(long timeTicks) {
		synchronized (this) {
			int index = 0;
			for (; index < this._fileStartIds.size() - 1; ++index) { // never delete last file.
				long startId = this._fileStartIds.get(index);
				String pathDat = Paths.get(this.getSessionHome()).resolve(startId + ".dat").toString();
				String pathIdx = Paths.get(this.getSessionHome()).resolve(startId + ".idx").toString();
				if (System.IO.File.GetLastWriteTime(pathDat).getTime() >= timeTicks) {
					break;
				}

				(new File(pathDat)).delete();
				(new File(pathIdx)).delete();
			}
			this._fileStartIds.subList(0, index).clear();
			this.setFirstId(!this._fileStartIds.isEmpty() ? this._fileStartIds.get(0) : 0);
		}
	}

	public void DeleteContentFileBefore(long time) {
		DeleteFileBefore(this.getContentHome(), time);
	}

	public void close() throws IOException {
		if (_lastDataFile != null) {
			_lastDataFile.close();
		}
	}

	private static class MessageFile implements Closeable {
		private long StartId;
		public final long getStartId() {
			return StartId;
		}
		private void setStartId(long value) {
			StartId = value;
		}
		private ChatHistory ChatHistory;
		public final ChatHistory getChatHistory() {
			return ChatHistory;
		}
		private void setChatHistory(ChatHistory value) {
			ChatHistory = value;
		}
		public final long getDataFileLength() {
			return data.Length;
		}

		private FileInputStream data;
		private FileInputStream index;

		public MessageFile(ChatHistory chatHistory, long startId) {
			this.setStartId(startId);
			this.setChatHistory(chatHistory);

			data = System.IO.File.Open(Paths.get(chatHistory.getSessionHome()).resolve(startId + ".dat").toString(), System.IO.FileMode.OpenOrCreate);
			index = System.IO.File.Open(Paths.get(chatHistory.getSessionHome()).resolve(startId + ".idx").toString(), System.IO.FileMode.OpenOrCreate);
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void WriteToTail(byte[] src, int offset, int length)
		public final void WriteToTail(byte[] src, int offset, int length) {
			long offsetData = data.Seek(0, System.IO.SeekOrigin.End);
			data.Write(src, offset, length);

			index.Seek(0, System.IO.SeekOrigin.End);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] offsetDataBytes = BitConverter.GetBytes(offsetData);
			byte[] offsetDataBytes = BitConverter.GetBytes(offsetData);
			index.Write(offsetDataBytes, 0, offsetDataBytes.length);
		}

		private long SeekDataOffset(long fromId) {
			if (fromId < getStartId()) {
				return -1;
			}

			long indexOffset = (fromId - getStartId()) * 8;
			if (indexOffset > index.Length - 8) {
				return -1;
			}

			// 先读取索引，定位数据文件。
			index.Seek(indexOffset, System.IO.SeekOrigin.Begin);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] offsetBytes = new byte[8];
			byte[] offsetBytes = new byte[8];
			int rlen = index.read(offsetBytes, 0, offsetBytes.length);
			if (rlen == 0) { // eof
				return -1;
			}
			if (rlen != offsetBytes.length) {
				throw new RuntimeException("read index error");
			}

			long dataOffset = BitConverter.ToInt64(offsetBytes, 0);
			return data.Seek(dataOffset, System.IO.SeekOrigin.Begin);
		}

		public final long Read(long fromId, long count, ArrayList<ChatHistoryMessage> result) {
			if (count <= 0) {
				return 0;
			}

			if (SeekDataOffset(fromId) < 0) {
				return 0;
			}

			long i = 0;
			for (; i < count; ++i) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] msgSizeBytes = new byte[4];
				byte[] msgSizeBytes = new byte[4];
				int msgSizeLen = data.read(msgSizeBytes, 0, msgSizeBytes.length);
				if (msgSizeLen == 0) { // eof
					break;
				}
				if (msgSizeBytes.length != msgSizeLen) {
					throw new RuntimeException("read size error");
				}

				int msgSize = BitConverter.ToInt32(msgSizeBytes, 0);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] msgDataBytes = new byte[msgSize];
				byte[] msgDataBytes = new byte[msgSize];
				if (msgDataBytes.length != data.read(msgDataBytes, 0, msgDataBytes.length)) {
					throw new RuntimeException("read data error");
				}

				Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Wrap(msgDataBytes);
				ChatHistoryMessage msg = new ChatHistoryMessage();
				msg.Decode(bb);
				if (msg.getId() != fromId + i) {
					throw new RuntimeException("msgId error");
				}

				result.add(msg);
			}

			return i;
		}

		public final void Delete(long id) {
			long offset = SeekDataOffset(id);
			if (offset < 0) {
				return;
			}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] head = new byte[4 + 4 + 9];
			byte[] head = new byte[4 + 4 + 9]; // size + tag + id
			int headLen = data.read(head, 0, head.length);
			if (headLen == 0) { // eof
				return;
			}

			Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Wrap(head, 0, headLen);
			int msgsize = bb.ReadInt4();
			int tag = bb.ReadInt4();
			long existid = bb.ReadLong();
			if (existid != id) {
				throw new RuntimeException("msgId error"); // report or ignore
			}

			tag |= ChatHistoryMessage.TagDeleted;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] newtagBytes = BitConverter.GetBytes(tag);
			byte[] newtagBytes = BitConverter.GetBytes(tag);
			long tagoffset = offset + 4;
			if (tagoffset != data.Seek(tagoffset, System.IO.SeekOrigin.Begin)) {
				throw new RuntimeException("seek error");
			}

			data.Write(newtagBytes, 0, 4);
		}

		public final void close() throws IOException {
			data.Dispose();
			index.Dispose();
		}
	}

	private MessageFile _lastDataFile;
	private ArrayList<Long> _fileStartIds; // sorted

	// -1 not found
	private int FindStartIdIndex(long curId) {
		int prev = -1;
		for (int i = 0; i < this._fileStartIds.size(); ++i) {
			if (curId < this._fileStartIds.get(i)) {
				return prev;
			}
			prev = i;
		}
		return prev;
	}
	private void LoadFileStartIdsAndInit() {
		this._fileStartIds = GetStartIds(this.getSessionHome());
		this.setFirstId(0);
		this.setLastId(0);
		for (long startId : this._fileStartIds) {
			this.setFirstId(startId);
			String path = Paths.get(this.getSessionHome()).resolve(startId + ".idx").toString();
			try (System.IO.FileStream indexFile = System.IO.File.Open(path, System.IO.FileMode.OpenOrCreate)) {
				if (indexFile.Length % 8 != 0) {
					throw new RuntimeException("wrong index file size.");
				}
				this.setLastId(startId + indexFile.Length / 8);
			}
		}

		OpenOrCreateLastDataFile(!this._fileStartIds.isEmpty() ? this._fileStartIds.get(_fileStartIds.size() - 1) : 0);
	}

	private void OpenOrCreateLastDataFile(long startId) {
		if (_lastDataFile != null) {
			_lastDataFile.close();
		}
		_lastDataFile = new MessageFile(this, startId);
		if (this._fileStartIds.contains(startId)) {
			return;
		}
		this._fileStartIds.add(startId);
	}

	public static ArrayList<Long> GetStartIds(String dir) {
		File dirInfo = new File(dir);
		File[] files = dirInfo.GetFiles("*.dat");
		ArrayList<Long> startIds = new ArrayList<Long>();
		for (File file : files) {
			int endpos = file.getName().indexOf('.');
			String fname = file.getName().substring(0, endpos);
			long startId = Long.parseLong(fname);
			if (startId < 0) {
				throw new RuntimeException("invalid start id: " + file.getName());
			}
			startIds.add(startId);
		}
		Collections.sort(startIds);
		return startIds;
	}

	public static void DeleteFileBefore(String dir, long timeTicks) {
		File dirInfo = new File(dir);
		File[] files = dirInfo.GetFiles("*");
		for (File file : files) {
			if (file.LastWriteTime.getTime() < timeTicks) {
				file.delete();
			}
		}
	}
}