using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Microsoft.VisualBasic;

namespace Zeze.Gen
{
    public class Program
    {
        private static Dictionary<string, Solution> solutions = new Dictionary<string, Solution>();

        public static Zeze.Util.Ranges GlobalModuleIdChecker { get; private set; } = new Zeze.Util.Ranges();

        public static bool Debug { get; private set; } = false;

        // 用来保存可命名对象（bean,protocol,rpc,table,Project,Manager,Module...)，用来 1 检查命名是否重复，2 查找对象。
        // key 为全名：包含完整的名字空间。
        public static SortedDictionary<string, object> NamedObjects { get; private set; } = new SortedDictionary<string, object>();

        public static void AddNamedObject(string fullName, object obj)
        {
            if (NamedObjects.ContainsKey(fullName))
                throw new Exception("duplicate name: " + fullName);
            NamedObjects.Add(fullName, obj);
        }

        public static T GetNamedObject<T>(string fullName)
        {
            object value = null;
            if (NamedObjects.TryGetValue(fullName, out value))
            {
                if (value is T)
                    return (T)value;
                throw new Exception("NamedObject is not " + fullName); // 怎么得到模板参数类型？
            }
            throw new Exception("NamedObject not found: " + fullName);
        }

        public static void ImportSolution(string xmlfile)
        {
            if (solutions.ContainsKey(xmlfile))
                return;

            XmlDocument doc = new XmlDocument();
            doc.Load(xmlfile);
            Solution solution = new Solution(doc.DocumentElement);
            foreach (KeyValuePair<string, Solution> exist in solutions)
            {
                if (exist.Value.Name.Equals(solution.Name))
                    Console.WriteLine("WARN duplicate solution name: " + solution.Name + " in file: " + exist.Key + "," + xmlfile);
            }
            solutions.Add(xmlfile, solution);
        }
        public static void Main(string[] args)
        {
            // UnitTest test. fixme.
            string path = "C:\\Users\\86139\\Desktop\\code\\zeze\\UnitTest\\solution.xml";
            string pathcur = System.IO.Path.Combine(System.Environment.CurrentDirectory, "solution.xml");
            System.IO.File.Delete(pathcur);
            System.IO.File.Copy(path, pathcur);

            List<string> xmlFileList = new List<string>();
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-debug":
                        Debug = true;
                        break;
                    default:
                        xmlFileList.Add(args[i]);
                        break;
                }
            }
            if (xmlFileList.Count == 0)
            {
                string xmlDefault = "solution.xml";
                if (false == System.IO.File.Exists(xmlDefault))
                {
                    Console.WriteLine(xmlDefault + " not found");
                }
                xmlFileList.Add(xmlDefault);
            }
            foreach (string file in xmlFileList)
            {
                ImportSolution(file);
            }

            foreach (Solution sol in solutions.Values) // compile all .包含从文件中 import 进来的。
            {
                sol.Compile();
            }

            foreach (string file in xmlFileList) // make 参数指定的 Solution
            {
                Solution sol = null;
                solutions.TryGetValue(file, out sol);
                sol.Make();
            }
        }

        public static List<Module> CompileModuleRef(List<string> fullNames)
        {
            List<Module> result = new List<Module>();
            foreach (string fullName in fullNames)
                result.Add(GetNamedObject<Module>(fullName));
            return result;
        }

        public static bool IsFullName(string name)
        {
            return name.IndexOf('.') != -1;
        }

        public static string ToFullNameIfNot(string path, string name)
        {
            return IsFullName(name) ? name : path + "." + name;
        }

        public static List<string> ToFullNameIfNot(string path, ICollection<string> names)
        {
            List<string> fullNames = new List<string>();
            foreach (string name in names)
            {
                fullNames.Add(ToFullNameIfNot(path, name));
            }
            return fullNames;
        }

        public static ICollection<String> Refs(XmlElement self, String nodename, String refName)
        {
            HashSet<String> refs = new HashSet<String>();
            XmlNodeList childnodes = self.ChildNodes;
            foreach (XmlNode node in childnodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                if (e.Name.Equals(nodename))
                {
                    if (false == refs.Add(e.GetAttribute(refName)))
                    {
                        throw new Exception("duplicate ref name " );
                    }
                }
            }
            return refs;
        }

        public static ICollection<String> Refs(XmlElement self, String nodename)
        {
            return Refs(self, nodename, "ref");
        }
    }
}
