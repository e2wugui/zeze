using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestZero
    {
        [TestMethod]
        public void Go()
        {
            demo.App.Instance.Start();
            try
            {
                demo.App.Instance.Zeze.NewProcedure(async () =>
                {
                    await demo.App.Instance.demo_Module1.Table1.Remove(1);
                    return 0;
                }, "go").Call();
            }
            finally
            {
                demo.App.Instance.Stop();
            }
        }
    }
}
