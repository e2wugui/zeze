using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Program
    {
        private static Dictionary<string, Solution> solutions = new Dictionary<string, Solution>();
        public static Zeze.Util.Ranges GlobalModuleIdChecker { get; private set; } = new Zeze.Util.Ranges();

        public static void ImportSolution(string xmlfile)
        {
            if (solutions.ContainsKey(xmlfile))
                return;

            XmlDocument doc = new XmlDocument();
            doc.Load(xmlfile);
            solutions.Add(xmlfile, new Solution(doc.DocumentElement));
        }
        public static void Main(string[] args)
        {
            string xmlFileName = "solution.xml";
            ImportSolution("C:\\Users\\86139\\Desktop\\code\\zeze\\UnitTest\\" + xmlFileName);
        }
    }
}
