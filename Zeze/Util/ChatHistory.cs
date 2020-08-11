using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.CompilerServices;
using System.Threading;
using Microsoft.VisualBasic.CompilerServices;

/// <summary>
/// *性能性能性能性能
/// 每条消息顺序存储，严重依赖写文件不能出错。
/// *写文件部分失败，导致不能工作。
/// *写两个文件，数据和索引不一致会导致不能工作。
/// 其中索引损坏可以从数据文件重建，
/// 数据文件顺坏，就涉及消息记录定位问题，目前定位能力比较弱。
/// </summary>
namespace Zeze.Util
{
    public class ChatHistory : IDisposable
    {
        public string HistoryHome { get; }
        public string SessionHome { get; }
        public string ContentHome { get; }
        public long SessionId { get; }
        public long MaxSingleDataFileLength { get; }
        public long SeparateContentLength { get; }
        public long LastId { get; private set; }
        public long FirstId { get; private set; }

        public ChatHistory(string historyHome, long sessionId, long maxSingleDataFileLength, long separateContentLength)
        {
            if (false == System.IO.Directory.Exists(historyHome))
                throw new ArgumentException("history home not exist.");

            this.HistoryHome = historyHome;
            this.SessionHome = System.IO.Path.Combine(historyHome, sessionId.ToString());
            this.ContentHome = System.IO.Path.Combine(this.SessionHome, "contents");
            System.IO.Directory.CreateDirectory(this.SessionHome);
            System.IO.Directory.CreateDirectory(this.ContentHome);

            this.SessionId = sessionId;
            this.MaxSingleDataFileLength = maxSingleDataFileLength;
            this.SeparateContentLength = separateContentLength;

            LoadFileStartIdsAndInit();
        }

        [MethodImpl(MethodImplOptions.AggressiveInlining)]
        public long AddMessage(string sender, string content)
        {
            return AddMessage(sender, ChatHistoryMessage.TypeString, Encoding.UTF8.GetBytes(content));
        }

        /// <summary>
        /// 增加消息到聊天历史中。
        /// </summary>
        /// <param name="sender">发送者</param>
        /// <param name="type">消息类型</param>
        /// <param name="content">消息内容</param>
        /// <returns></returns>
        public long AddMessage(string sender, int type, byte[] content)
        {
            if (type < 0)
                throw new ArgumentException("type is reserved: " + type);

            lock (this)
            {
                ChatHistoryMessage msg = new ChatHistoryMessage
                {
                    Tag = 0,
                    Id = LastId,
                    TimeTicks = DateTime.Now.Ticks,
                    Sender = sender,
                    Type = type,
                    Content = content
                };

                if (this.SeparateContentLength > 0 && content.Length > this.SeparateContentLength)
                {
                    msg.Tag = ChatHistoryMessage.TagSeparate;
                    msg.SaveContentToFile(System.IO.Path.Combine(this.ContentHome, msg.Id.ToString()));
                    msg.Content = Array.Empty<byte>(); // 对于图片视频，这里可以考虑放一个缩小的提示性图片。
                }

                if (this.MaxSingleDataFileLength > 0 && _lastDataFile.DataFileLength > this.MaxSingleDataFileLength)
                    OpenOrCreateLastDataFile(LastId);

                Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Allocate(msg.SizeHint());
                int savedWriteIndex = bb.WriteIndex;
                bb.Append(new byte[4]); // prepare for size bytes
                msg.Encode(bb);
                bb.Replace(savedWriteIndex, BitConverter.GetBytes(bb.Size - 4));
                _lastDataFile.WriteToTail(bb.Bytes, bb.ReadIndex, bb.Size);

                return LastId++; // 最后才真的增加，避免上面异常导致LastId已被增加。 
            }
        }

        /// <summary>
        /// 读取最近的几条消息。
        /// </summary>
        /// <param name="count">消息个数，-1 表示读取全部</param>
        /// <returns></returns>
        public List<ChatHistoryMessage> ReadMessageRecent(int count)
        {
            if (count < 0) // read all
                return ReadMessage(FirstId, -1);

            long fromId = LastId - count;
            if (fromId < FirstId)
                fromId = FirstId;

            return ReadMessage(fromId, count);
        }
        /// <summary>
        /// 从 指定fromId 开始顺序读取一定数量的消息。
        /// </summary>
        /// <param name="fromId"> 开始的Id，包含。如果fromId所在的数据已经被删除，返回空List。外面根据需要可以判断FirstId修正，内部不自动修正。 </param>
        /// <param name="count"> 读取数量，-1 一直读到结尾。</param>
        /// <returns></returns>
        public List<ChatHistoryMessage> ReadMessage(long fromId, int count)
        {
            if (fromId < 0)
                throw new ArgumentException();

            List<ChatHistoryMessage> result = new List<ChatHistoryMessage>();

            long countReal = count; // 内部使用long类型
            if (countReal < 0)
                countReal = LastId - fromId;
            if (countReal < 0)
                return result;

            lock (this)
            {
                int startIdIndex = FindStartIdIndex(fromId);
                if (startIdIndex < 0)
                    return result;

                for (; countReal > 0 && startIdIndex < _fileStartIds.Count; ++startIdIndex)
                {
                    long startId = _fileStartIds[startIdIndex];
                    if (_lastDataFile.StartId == startId)
                    {
                        //int realReadCount = 
                        _lastDataFile.Read(fromId, countReal, result);
                        return result; // 最后一个文件，读多少算多少，直接返回。
                    }

                    using (MessageFile mf = new MessageFile(this, startId))
                    {
                        long r = mf.Read(fromId, count, result);
                        if (r > 0)
                        {
                            countReal -= r;
                            fromId = result[^1].Id + 1;
                        }
                    }
                }
            }
            return result;
        }

        public void DeleteMessage(long id)
        {
            int startIdIndex = FindStartIdIndex(id);
            if (startIdIndex < 0)
                return;

            long startId = _fileStartIds[startIdIndex];
            if (_lastDataFile.StartId == startId)
            {
                _lastDataFile.Delete(id);
                return;
            }

            using (MessageFile mf = new MessageFile(this, startId))
            {
                mf.Delete(id);
            }
        }

        public byte[] LoadContentFromFile(long id)
        {
            return ChatHistoryMessage.LoadContentFromFile(System.IO.Path.Combine(this.ContentHome, id.ToString()));
        }

        /// <summary>
        /// 删除最后写文件的时间在time以前的数据和索引文件。
        /// 最后一个（当前）文件不会被删除: 因为要记住当前的msgId。全部清除会导致msgId重新从0开始。
        /// </summary>
        /// <param name="timeTicks"></param>
        public void DeleteFileBefore(long timeTicks)
        {
            lock(this)
            {
                int index = 0;
                for (; index < this._fileStartIds.Count - 1; ++index) // never delete last file.
                {
                    long startId = this._fileStartIds[index];
                    string pathDat = System.IO.Path.Combine(this.SessionHome, startId + ".dat");
                    string pathIdx = System.IO.Path.Combine(this.SessionHome, startId + ".idx");
                    if (System.IO.File.GetLastWriteTime(pathDat).Ticks >= timeTicks)
                        break;

                    System.IO.File.Delete(pathDat);
                    System.IO.File.Delete(pathIdx);
                }
                this._fileStartIds.RemoveRange(0, index);
                this.FirstId = this._fileStartIds.Count > 0 ? this._fileStartIds[0] : 0;
            }
        }

        public void DeleteContentFileBefore(long time)
        {
            DeleteFileBefore(this.ContentHome, time);
        }

        public void Dispose()
        {
            _lastDataFile?.Dispose();
        }

        private class MessageFile : IDisposable
        {
            public long StartId { get; private set; }
            public ChatHistory ChatHistory { get; private set; }
            public long DataFileLength { get { return data.Length;  } }

            private System.IO.FileStream data;
            private System.IO.FileStream index;

            public MessageFile(ChatHistory chatHistory, long startId)
            {
                this.StartId = startId;
                this.ChatHistory = chatHistory;

                data = System.IO.File.Open(System.IO.Path.Combine(chatHistory.SessionHome, startId + ".dat"), System.IO.FileMode.OpenOrCreate);
                index = System.IO.File.Open(System.IO.Path.Combine(chatHistory.SessionHome, startId + ".idx"), System.IO.FileMode.OpenOrCreate);
            }

            public void WriteToTail(byte[] src, int offset, int length)
            {
                long offsetData = data.Seek(0, System.IO.SeekOrigin.End);
                data.Write(src, offset, length);
                
                index.Seek(0, System.IO.SeekOrigin.End);
                byte[] offsetDataBytes = BitConverter.GetBytes(offsetData);
                index.Write(offsetDataBytes, 0, offsetDataBytes.Length);
            }

            private long SeekDataOffset(long fromId)
            {
                if (fromId < StartId)
                    return -1;

                long indexOffset = (fromId - StartId) * 8;
                if (indexOffset > index.Length - 8)
                    return -1;

                // 先读取索引，定位数据文件。
                index.Seek(indexOffset, System.IO.SeekOrigin.Begin);
                byte[] offsetBytes = new byte[8];
                int rlen = index.Read(offsetBytes, 0, offsetBytes.Length);
                if (rlen == 0) // eof
                    return -1;
                if (rlen != offsetBytes.Length)
                    throw new Exception("read index error");

                long dataOffset = BitConverter.ToInt64(offsetBytes);
                return data.Seek(dataOffset, System.IO.SeekOrigin.Begin);
            }

            public long Read(long fromId, long count, List<ChatHistoryMessage> result)
            {
                if (count <= 0)
                    return 0;

                if (SeekDataOffset(fromId) < 0)
                    return 0;

                long i = 0;
                for (; i < count; ++i)
                {
                    byte[] msgSizeBytes = new byte[4];
                    int msgSizeLen = data.Read(msgSizeBytes, 0, msgSizeBytes.Length);
                    if (msgSizeLen == 0) // eof
                        break;
                    if (msgSizeBytes.Length != msgSizeLen)
                        throw new Exception("read size error");

                    int msgSize = BitConverter.ToInt32(msgSizeBytes);
                    byte[] msgDataBytes = new byte[msgSize];
                    if (msgDataBytes.Length != data.Read(msgDataBytes, 0, msgDataBytes.Length))
                        throw new Exception("read data error");

                    Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Wrap(msgDataBytes);
                    ChatHistoryMessage msg = new ChatHistoryMessage();
                    msg.Decode(bb);
                    if (msg.Id != fromId + i)
                        throw new Exception("msgId error");

                    result.Add(msg);
                }

                return i;
            }

            public void Delete(long id)
            {
                long offset = SeekDataOffset(id);
                if (offset < 0)
                    return;

                byte[] head = new byte[4 + 4 + 9]; // size + tag + id
                int headLen = data.Read(head, 0, head.Length);
                if (headLen == 0) // eof
                    return;

                Zeze.Serialize.ByteBuffer bb = Zeze.Serialize.ByteBuffer.Wrap(head, 0, headLen);
                int msgsize = bb.ReadInt4();
                int tag = bb.ReadInt4();
                long existid = bb.ReadLong();
                if (existid != id)
                    throw new Exception("msgId error"); // report or ignore

                tag |= ChatHistoryMessage.TagDeleted;
                byte[] newtagBytes = BitConverter.GetBytes(tag);
                long tagoffset = offset + 4;
                if (tagoffset != data.Seek(tagoffset, System.IO.SeekOrigin.Begin))
                    throw new Exception("seek error");

                data.Write(newtagBytes, 0, 4);
            }

            public void Dispose()
            {
                data.Dispose();
                index.Dispose();
            }
        }

        private MessageFile _lastDataFile;
        private List<long> _fileStartIds; // sorted

        // -1 not found
        private int FindStartIdIndex(long curId)
        {
            int prev = -1;
            for (int i = 0; i < this._fileStartIds.Count; ++i)
            {
                if (curId < this._fileStartIds[i])
                    return prev;
                prev = i;
            }
            return prev;
        }
        private void LoadFileStartIdsAndInit()
        {
            this._fileStartIds = GetStartIds(this.SessionHome);
            this.FirstId = 0;
            this.LastId = 0;
            foreach (long startId in this._fileStartIds)
            {
                this.FirstId = startId;
                string path = System.IO.Path.Combine(this.SessionHome, startId + ".idx");
                using (System.IO.FileStream indexFile = System.IO.File.Open(path, System.IO.FileMode.OpenOrCreate))
                {
                    if (indexFile.Length % 8 != 0)
                        throw new Exception("wrong index file size.");
                    this.LastId = startId + indexFile.Length / 8;
                }
            }

            OpenOrCreateLastDataFile(this._fileStartIds.Count > 0 ? this._fileStartIds[^1] : 0);
        }

        private void OpenOrCreateLastDataFile(long startId)
        {
            _lastDataFile?.Dispose();
            _lastDataFile = new MessageFile(this, startId);
            if (this._fileStartIds.Contains(startId))
                return;
            this._fileStartIds.Add(startId);
        }

        public static List<long> GetStartIds(string dir)
        {
            System.IO.DirectoryInfo dirInfo = new System.IO.DirectoryInfo(dir);
            System.IO.FileInfo[] files = dirInfo.GetFiles("*.dat");
            List<long> startIds = new List<long>();
            foreach (System.IO.FileInfo file in files)
            {
                int endpos = file.Name.IndexOf('.');
                string fname = file.Name.Substring(0, endpos);
                long startId = long.Parse(fname);
                if (startId < 0)
                    throw new Exception("invalid start id: " + file.Name);
                startIds.Add(startId);
            }
            startIds.Sort();
            return startIds;
        }

        public static void DeleteFileBefore(string dir, long timeTicks)
        {
            System.IO.DirectoryInfo dirInfo = new System.IO.DirectoryInfo(dir);
            System.IO.FileInfo[] files = dirInfo.GetFiles("*");
            foreach (System.IO.FileInfo file in files)
            {
                if (file.LastWriteTime.Ticks < timeTicks)
                    file.Delete();
            }
        }
    }
}
