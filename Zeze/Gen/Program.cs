using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Program
    {
        private static Dictionary<string, Solution> solutions = new Dictionary<string, Solution>();
        public static Zeze.Util.AtomicLong IdGen = new Zeze.Util.AtomicLong();

        public static string GenUniqVarName()
        {
            return "_v_" + IdGen.IncrementAndGet() + "_";
        }

        public static global::Zeze.Util.Ranges GlobalModuleIdChecker { get; private set; } = new global::Zeze.Util.Ranges();

        public static bool Debug { get; private set; } = false;

        // 用来保存可命名对象（bean,protocol,rpc,table,Project,Service,Module...)，用来 1 检查命名是否重复，2 查找对象。
        // key 为全名：包含完整的名字空间。
        public static SortedDictionary<string, object> NamedObjects { get; private set; } = new SortedDictionary<string, object>();
        public static HashSet<long> BeanTypeIdDuplicateChecker { get; } = new HashSet<long>();

        public static void AddNamedObject(string fullName, object obj)
        {
            // 由于创建文件在 windows 下大小写不敏感，所以名字需要大小写不敏感。
            string lower = fullName.ToLower();
            if (NamedObjects.ContainsKey(lower))
                throw new Exception("duplicate name(Not Case Sensitive): " + fullName);

            NamedObjects.Add(lower, obj);
        }

        public static T GetNamedObject<T>(string fullName)
        {
            string lower = fullName.ToLower();

            object value = null;
            if (NamedObjects.TryGetValue(lower, out value))
            {
                if (value is T)
                    return (T)value;
                throw new Exception("NamedObject is not " + fullName); // 怎么得到模板参数类型？
            }
            throw new Exception("NamedObject not found(Not Case Sensitive): " + fullName);
        }

        public static void ImportSolution(string xmlfile)
        {
            if (solutions.ContainsKey(xmlfile))
                return;

            XmlDocument doc = new XmlDocument();
            doc.Load(xmlfile);
            Solution solution = new Solution(doc.DocumentElement);
            /*
            foreach (KeyValuePair<string, Solution> exist in solutions)
            {
                if (exist.Value.Name.Equals(solution.Name))
                    Console.WriteLine("WARN duplicate solution name: " + solution.Name + " in file: " + exist.Key + "," + xmlfile);
            }
            */
            solutions.Add(xmlfile, solution);
        }
        public static void Main(string[] args)
        {
            BeanTypeIdDuplicateChecker.Add(Zeze.Transaction.EmptyBean.TYPEID);

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

        public static void Print(object obj)
        {
            Console.WriteLine(obj);
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

        public static int HandleServerFlag = 1;
        public static int HandleClientFlag = 2;
        public static int HandleScriptServerFlag = 8;
        public static int HandleScriptClientFlag = 16;
        public static int HandleCSharpFlags = HandleServerFlag | HandleClientFlag; // 底层语言。如果c++需要生成协议之类的，也是用这个。
        public static int HandleScriptFlags = HandleScriptServerFlag | HandleScriptClientFlag;

        public static int ToHandleFlags(string handle)
        {
            int f = 0;
            string hs = handle.Trim().ToLower();
            foreach (string h in hs.Split(','))
            {
                switch (h.Trim())
                {
                    case "server": f |= HandleServerFlag; break;
                    case "client": f |= HandleClientFlag; break;
                    case "serverscript": f |= HandleScriptServerFlag; break;
                    case "clientscript": f |= HandleScriptClientFlag; break;
                    default: throw new Exception("unknown handle: " + handle);
                }
            }
            return f;
        }

        public static System.IO.StreamWriter OpenWriterNoPath(string baseDir, string fileName, bool overwrite = true)
        {
            System.IO.Directory.CreateDirectory(baseDir);
            string fullFileName = System.IO.Path.Combine(baseDir, fileName);
            bool exists = System.IO.File.Exists(fullFileName);
            if (!exists || overwrite)
            {
                Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
                System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
                return sw;
            }
            Program.Print("file skip '" + fullFileName + "'");
            return null;
        }

    }
}
