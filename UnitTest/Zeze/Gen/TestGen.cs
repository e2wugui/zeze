using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Util;

namespace UnitTest.Zeze.Gen
{
    [TestClass]
    public class TestGen
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(TestGen));

        [TestMethod]
        public void TestMain()
        {
            // UnitTest test. fixme.
            //System.Environment.CurrentDirectory = "C:\\Users\\86139\\Desktop\\code\\zeze\\UnitTest\\";
            /*
            string path = "C:\\Users\\86139\\Desktop\\code\\zeze\\UnitTest\\solution.xml";
            string pathcur = System.IO.Path.Combine(System.Environment.CurrentDirectory, "solution.xml");
            System.IO.File.Delete(pathcur);
            System.IO.File.Copy(path, pathcur);
            */
            //Program.Main(new string[]{ "solution.xml" });
            try
            {
                throw1();
            }
            catch (Exception ex)
            {
                logger.Error(ex, "msg");
            }
        }

        private void throw1()
        {
            throw new Exception("exp");
        }
    }
}
