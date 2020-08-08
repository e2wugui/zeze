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
        public void TestAdd()
        {
            using (ChatHistory ch = new ChatHistory(".", 0, -1, -1))
            {
                long msgId = ch.AddMessage("sender", "content");
                List<ChatHistoryMessage> msgs = ch.ReadMessage(msgId, 1);
                foreach (ChatHistoryMessage msg in msgs)
                {
                    Console.WriteLine(msg.Sender + " " + msg.Content);
                }
            }
        }
    }
}
