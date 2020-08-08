using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;

using Zeze.Util;

namespace UnitTest.Zeze.Util
{
    [TestClass]
    public class TestChatHistory
    {
        [TestMethod]
        public void TestDeleteFile()
        {
            using ChatHistory ch = new ChatHistory(".", 0, -1, -1);
            long now = DateTime.Now.Ticks;
            ch.DeleteFileBefore(now);
            ch.DeleteContentFileBefore(now);
        }

        [TestMethod]
        public void TestAddDelete()
        {
            // 数据存储在文件中，每次运行只测试新加消息。
            using ChatHistory ch = new ChatHistory(".", 0, -1, -1);
            List<long> addedIds = new List<long>();
            for (int i = 0; i < 10; ++i)
            {
                addedIds.Add(ch.AddMessage("" + i, "" + i));
            }
            List<long> deletedIds = new List<long>();
            foreach (long id in addedIds)
            {
                if (id % 2 == 0)
                    continue;
                ch.DeleteMessage(id);
                deletedIds.Add(id);
            }

            for (int i = 0; i < addedIds.Count; ++i)
            {
                long id = addedIds[i];
                List<ChatHistoryMessage> msgs = ch.ReadMessage(id, 1);
                Assert.IsTrue(msgs.Count == 1);
                ChatHistoryMessage msg = msgs[^1];
                Assert.AreEqual(id, msg.Id);
                Assert.AreEqual("" + i, msg.Sender);
                Assert.AreEqual("" + i, msg.ContentStr);
                if (id % 2 != 0)
                    Assert.AreEqual(true, msg.IsDeleted);
            }
        }

        [TestMethod]
        public void TestRotateSeparate()
        {
            using ChatHistory ch = new ChatHistory(".", 0, 128, 8);
            List<long> addedIds = new List<long>();
            string sender = "RotateSeparate";
            string content = "bigcontentbigcontentbigcontent";
            for (int i = 0; i < 6; ++i)
            {
                addedIds.Add(ch.AddMessage(sender, content));
            }
            List<ChatHistoryMessage> msgs = ch.ReadMessage(ch.FirstId, -1);
            foreach (ChatHistoryMessage msg in msgs)
            {
                if (addedIds.Contains(msg.Id))
                {
                    Assert.AreEqual(sender, msg.Sender);
                    Assert.IsTrue(msg.IsSeparate);
                    string load = System.Text.Encoding.UTF8.GetString(ch.LoadContentFromFile(msg.Id));
                    Assert.AreEqual(content, load);
                }
                //Console.WriteLine(msg.Id + "(" + msg.IsDeleted + "," + msg.IsSeparate + ")" + msg.Sender + " " + msg.ContentStr);
            }
        }

    }
}
