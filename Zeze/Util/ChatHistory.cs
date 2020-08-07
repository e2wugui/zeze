using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public class ChatHistory
    {
        private long sessionId;
        private long lastId = 0;

        public long LastId { get; }
        public long SessionId { get; }

        public ChatHistory(string fileDir, long sessionId)
        {
            this.sessionId = sessionId;
        }

        public long AddMessage(string sender, int type, byte[] content)
        {
            ChatHistoryMessage msg = new ChatHistoryMessage();
            msg.Id = ++lastId;
            msg.Tag = 0;
            msg.Time = DateTime.Now.Ticks;
            msg.Sender = sender;
            msg.Type = type;
            msg.Content = content;

            Zeze.Serialize.ByteBuffer bb = ((Zeze.Serialize.Serializable)msg).Encode();

            System.IO.FileStream last = OpenLastDataFile();
            last.Seek(0, System.IO.SeekOrigin.End);
            //last.Write(array, 0, count);

            return lastId;
        }
        /*
        public List<Message> ReadMessageAfter(long id, int maxCount)
        {
        }

        public List<Message> ReadMessageAfter(long id)
        {
            return ReadMessageAfter(id, lastId - id);
        }
        */

        public void DeleteMessage(long id)
        {
        }

        public void Clear()
        {
        }

        public void DeleteFileBefore(long time)
        {
        }

        private string fileDir;
        private System.IO.FileStream lastFileStream;
        //private string     fileNameReal;

        private string[] ListFileNames()
        {
            return null;
        }

        private System.IO.FileStream OpenLastDataFile()
        {
            return lastFileStream;
        }
    }
}
