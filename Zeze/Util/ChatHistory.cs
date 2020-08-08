using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.CompilerServices;
using System.Threading;
using Microsoft.VisualBasic.CompilerServices;

/// <summary>
/// TODO AddMessage 
/// 1 检查文件是否损坏（offset 不正确）
/// 2 写两个文件，部分失败，数据和索引不一致。
/// </summary>
namespace Zeze.Util
{
    public class ChatHistory : IDisposable
    {
        public string HistoryHome { get; }
        public string SessionHome { get; }
        public long SessionId { get; }
        public long MaxSingleDataFileLength { get; }
        public long SeparateContentLength { get; }
        public long LastId { get { return lastId; } }

        public ChatHistory(string historyHome, long sessionId, long maxSingleDataFileLength, long separateContentLength)
        {
            if (false == System.IO.Directory.Exists(historyHome))
                throw new ArgumentException("history home not exist.");

            this.HistoryHome = historyHome;
            this.SessionHome = System.IO.Path.Combine(historyHome, sessionId.ToString());
            System.IO.Directory.CreateDirectory(this.SessionHome);

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
                    Id = lastId,
                    Time = DateTime.Now.Ticks,
                    Sender = sender,
                    Type = type,
                    Content = content
                };

                Zeze.Serialize.ByteBuffer bb = new Zeze.Serialize.ByteBuffer(msg.SizeHint());
                int savedWriteIndex = bb.WriteIndex;
                bb.Append(new byte[4]); // prepare for size bytes
                msg.Encode(bb);
                bb.Replace(savedWriteIndex, BitConverter.GetBytes(bb.Size - 4));
                lastDataFile.Write(lastId, bb.Bytes, bb.ReadIndex, bb.Size);

                return lastId++; // 最后才真的增加，避免上面异常导致lastId已被增加。
            }
        }
 
        /// <summary>
        /// 从 指定fromId 开始顺序读取一定数量的消息。
        /// </summary>
        /// <param name="fromId"> 开始的Id，包含 </param>
        /// <param name="count"> 读取数量，-1 一直读到结尾。</param>
        /// <returns></returns>
        public List<ChatHistoryMessage> ReadMessage(long fromId, int count)
        {
            if (fromId < 0)
                throw new ArgumentException();

            List<ChatHistoryMessage> result = new List<ChatHistoryMessage>();

            lock (this)
            {
                if (count < 0)
                    count = (int)(lastId - fromId);

                int startIdIndex = FindStartIdIndex(fromId);
                if (startIdIndex < 0)
                    return result;

                for (; count > 0 && startIdIndex < fileStartIds.Count; ++startIdIndex)
                {
                    long startId = fileStartIds[startIdIndex];
                    if (null != lastDataFile && lastDataFile.StartId == startId)
                    {
                        //int realReadCount = 
                        lastDataFile.Read(fromId, count, result);
                        return result; // 最后一个文件，读多少算多少，直接返回。
                    }

                    using (MessageFile mf = new MessageFile(this, startId))
                    {
                        int r = mf.Read(fromId, count, result);
                        if (r > 0)
                        {
                            count -= r;
                            fromId = result[^1].Id + 1;
                        }
                    }
                }
            }
            return result;
        }

        public void DeleteMessage(long id)
        {
        }
 
        public void DeleteFileBefore(long time)
        {
        }

        public void Dispose()
        {
            lastDataFile?.Dispose();
        }

        private class MessageFile : IDisposable
        {
            public long StartId { get; private set; }
            public ChatHistory ChatHistory { get; private set; }

            private System.IO.FileStream data;
            private System.IO.FileStream index;

            public MessageFile(ChatHistory chatHistory, long startId)
            {
                this.StartId = startId;
                this.ChatHistory = chatHistory;

                data = System.IO.File.Open(System.IO.Path.Combine(chatHistory.SessionHome, startId + ".dat"), System.IO.FileMode.OpenOrCreate);
                index = System.IO.File.Open(System.IO.Path.Combine(chatHistory.SessionHome, startId + ".idx"), System.IO.FileMode.OpenOrCreate);
            }

            public void Write(long lastId, byte[] src, int offset, int length)
            {
                long offsetData = data.Seek(0, System.IO.SeekOrigin.End);
                data.Write(src, offset, length);

                index.Seek(lastId * 8, System.IO.SeekOrigin.Begin);
                byte[] offsetDataBytes = BitConverter.GetBytes(offsetData);
                index.Write(offsetDataBytes, 0, offsetDataBytes.Length);
            }

            public int Read(long fromId, int count, List<ChatHistoryMessage> result)
            {
                if (count <= 0)
                    return 0;

                if (fromId < StartId)
                    return 0;

                long indexOffset = (fromId - StartId) * 8;

                if (indexOffset > index.Length - 8)
                    return 0;

                // 先读取索引，当定位到数据文件后，按顺序读取，不再依赖索引文件。
                index.Seek(indexOffset, System.IO.SeekOrigin.Begin);
                byte[] offsetBytes = new byte[8];
                int rlen = index.Read(offsetBytes, 0, offsetBytes.Length);
                if (rlen == 0) // eof
                    return 0;
                if (rlen != offsetBytes.Length)
                    throw new Exception("read index error");

                long dataOffset = BitConverter.ToInt64(offsetBytes);
                data.Seek(dataOffset, System.IO.SeekOrigin.Begin);

                int i = 0;
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
            public void Dispose()
            {
                data.Close();
                index.Close();
            }
        }

        private long lastId = 0;
        private MessageFile lastDataFile;
        private List<long> fileStartIds; // sorted

        // -1 not found
        private int FindStartIdIndex(long curId)
        {
            int prev = -1;
            for (int i = 0; i < this.fileStartIds.Count; ++i)
            {
                if (curId < this.fileStartIds[i])
                    return prev;
                prev = i;
            }
            return prev;
        }
        private void LoadFileStartIdsAndInit()
        {
            this.fileStartIds = GetStartIds(this.SessionHome);
            this.lastId = 0;
            foreach (long startId in this.fileStartIds)
            {
                string path = System.IO.Path.Combine(this.SessionHome, startId + ".idx");
                using (System.IO.FileStream indexFile = System.IO.File.Open(path, System.IO.FileMode.OpenOrCreate))
                {
                    if (indexFile.Length % 8 != 0)
                        throw new Exception("wrong index file size.");
                    this.lastId = startId + indexFile.Length / 8;
                }
            }

            OpenLastDataFile(this.fileStartIds.Count > 0 ? this.fileStartIds[^1] : 0);
        }

        private void OpenLastDataFile(long startId)
        {
            lastDataFile?.Dispose();
            lastDataFile = new MessageFile(this, startId);
            if (this.fileStartIds.Contains(startId))
                return;
            this.fileStartIds.Add(startId);
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
    }
}
