using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace UnitTest.Zeze.Lua
{
    [TestClass]
    public class Main
    {
        // 用来启动服务器，测试网络，平时把下面这样注释掉
        [TestMethod]
        public void MainLoop()
        {
            demo.App.Instance.Start();
            try
            {
                while (true)
                {
                    System.Threading.Thread.Sleep(1000);
                }
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }
    }
}
