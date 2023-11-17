using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;
using Zeze.Gen.Types;
using Zeze.Util;

namespace Zeze.Gen
{
    public static class Program
    {
        public static Dictionary<string, Solution> Solutions { get; } = new();
        public static readonly AtomicLong IdGen = new();
        public static readonly Regex namePattern = new("^[\\w_]+$");
        public static readonly Regex fullNamePattern = new("^[\\w_.]+$");

        // 收集了Java,C#,C++,Javascript,Typescript,Lua的保留字(不能用作变量名)
        public static readonly HashSet<string> reservedNames = new()
        {
            "abstract", "alignas", "alignof", "and", "and_eq", "arguments", "as", "asm", "assert", "atomic_cancel",
            "atomic_commit", "atomic_noexcept", "auto", "await", "base", "bitand", "bitor", "bool", "boolean", "break",
            "byte", "case", "catch", "char", "char16_t", "char32_t", "char8_t", "checked", "class", "co_await",
            "co_return", "co_yield", "compl", "concept", "const", "const_cast", "consteval", "constexpr", "constinit",
            "continue", "debugger", "decimal", "decltype", "default", "delegate", "delete", "do", "double",
            "dynamic_cast", "else", "elseif", "end", "enum", "eval", "event", "explicit", "export", "extends", "extern",
            "false", "final", "finally", "fixed", "float", "for", "foreach", /*"friend",*/ "function", "goto", "if",
            "implements", "implicit", "import", "in", "inline", "instanceof", "int", "interface", "internal", "is",
            "let", "local", "lock", "long", "mutable", "namespace", "native", "new", "nil", "noexcept", "not", "not_eq",
            "null", "nullptr", "object", "operator", "or", "or_eq", "out", "override", "package", "params", "private",
            "protected", "public", "readonly", "ref", "reflexpr", "register", "reinterpret_cast", "repeat", "requires",
            "return", "sbyte", "sealed", "short", "signed", "sizeof", "stackalloc", "static", "static_assert",
            "static_cast", "strictfp", "string", "struct", "super", "switch", "synchronized", "template", "then",
            "this", "thread_local", "throw", "throws", "transient", "true", "try", "typedef", "typeid", "typename",
            "typeof", "uint", "ulong", "unchecked", "union", "unsafe", "unsigned", "until", "ushort", "using", "var",
            "virtual", "void", "volatile", "wchar_t", "while", "with", "xor", "xor_eq", "yield"
        };

        public static void CheckReserveName(string name, string path, bool hot = false)
        {
            if (name.StartsWith("_"))
                throw new Exception($"Name Can Not StartsWith '_': name='{name}' in '{path ?? ""}'");
            if (name.EndsWith("_") && !hot) // allow hot bean
                throw new Exception($"Name Can Not EndsWith '_': name='{name}' in '{path ?? ""}'");
            if (!namePattern.IsMatch(name))
                throw new Exception($"Name Can Not Contains Invalid Chars: name='{name}' in '{path ?? ""}'");
            if (reservedNames.Contains(name))
                throw new Exception($"Name Can Not Be Reserved Keyword: name='{name}' in '{path ?? ""}'");
        }

        public static void CheckReserveFullName(string name, string path)
        {
            if (name.StartsWith("_"))
                throw new Exception($"Name Can Not Starts With '_': name='{name}' in '{path ?? ""}'");
            if (name.EndsWith("_"))
                throw new Exception($"Name Can Not Ends With '_': name='{name}' in '{path ?? ""}'");
            if (!fullNamePattern.IsMatch(name))
                throw new Exception($"Name Can Not Contains Invalid Chars: name='{name}' in '{path ?? ""}'");
            if (reservedNames.Contains(name))
                throw new Exception($"Name Can Not Be Reserved Keyword: name='{name}' in '{path ?? ""}'");
        }

        public static void CheckValidName(string name, string path)
        {
            if (name.StartsWith("_"))
                throw new Exception($"Name Can Not Starts With '_': name='{name}' in '{path ?? ""}'");
            if (name.EndsWith("_"))
                throw new Exception($"Name Can Not Ends With '_': name='{name}' in '{path ?? ""}'");
            if (!namePattern.IsMatch(name))
                throw new Exception($"Name Can Not Contains Invalid Chars: name='{name}' in '{path ?? ""}'");
        }

        public static string GenUniqVarName()
        {
            return "_v_" + IdGen.IncrementAndGet() + "_";
        }

        // 第一个字母转换成大写。
        public static string Upper1(string name)
        {
            return string.Concat(name[..1].ToUpper(), name.AsSpan(1));
        }

        // 把 '.' 分隔的全路径名，最后一个名字的首字母转换成大写。
        public static string Upper1LastName(string fullName)
        {
            var lastIndex = fullName.LastIndexOf('.');
            if (lastIndex >= 0)
                return fullName.Substring(0, lastIndex + 1) + Upper1(fullName.Substring(lastIndex + 1));
            return fullName;
        }

        public static Ranges GlobalModuleIdChecker { get; private set; } = new();

        public static bool Debug { get; private set; }
        public static bool DeleteOldFile { get; private set; } = true;

        // 用来保存可命名对象（bean,protocol,rpc,table,Project,Service,Module...)，用来 1 检查命名是否重复，2 查找对象。
        // key 为全名：包含完整的名字空间。
        public static SortedDictionary<string, object> NamedObjects { get; } = new();
        public static HashSet<long> BeanTypeIdDuplicateChecker { get; } = new();
        public static HashSet<Types.Type> DataBeans { get; } = new(); // 所有的UseData的协议以来的类型。生成的时候需要过滤掉不是bean以及不是beankey的。

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

            if (NamedObjects.TryGetValue(lower, out var value))
            {
                if (value is T)
                    return (T)value;
                throw new Exception("NamedObject is not " + fullName); // 怎么得到模板参数类型？
            }
            throw new Exception("NamedObject not found(Not Case Sensitive): " + fullName);
        }

        static void GenDerives()
        {
            foreach (var e in NamedObjects)
            {
                if (e.Value is Bean bean)
                {
                    for (var baseName = bean.Base; baseName != "";)
                    {
                        if (!NamedObjects.TryGetValue(baseName.ToLower(), out var baseType) || baseType is not Bean baseBean)
                            break;
                        baseBean.Derives.Add(bean.FullName);
                        baseName = baseBean.Base;
                    }
                }
            }
        }

        private static List<string> SavedCurrentDirectory = new List<string>();

        public static void ImportSolution(string xmlFile)
        {
            var fullXmlFile = Path.GetFullPath(xmlFile);

            if (Solutions.ContainsKey(fullXmlFile))
                return;

            if (!File.Exists(fullXmlFile) && SavedCurrentDirectory.Count > 0)
            {
                fullXmlFile = Path.GetFullPath(Path.Combine(SavedCurrentDirectory[0], xmlFile));
                if (Solutions.ContainsKey(fullXmlFile))
                    return;

                if (!File.Exists(fullXmlFile))
                    throw new FileNotFoundException(xmlFile);
            }

            if (Debug)
                Console.WriteLine($"ImportSolution '{fullXmlFile}'");

            Solutions.Add(fullXmlFile, null); // 循环依赖。
            SavedCurrentDirectory.Add(Environment.CurrentDirectory);
            Environment.CurrentDirectory = Path.GetDirectoryName(fullXmlFile)!;
            XmlDocument doc = new XmlDocument();
            doc.Load(fullXmlFile);
            Solution solution = new Solution(doc.DocumentElement);
            /*
            foreach (KeyValuePair<string, Solution> exist in solutions)
            {
                if (exist.Value.Name.Equals(solution.Name))
                    Console.WriteLine("WARN duplicate solution name: " + solution.Name + " in file: " + exist.Key + "," + xmlFile);
            }
            */
            Solutions[fullXmlFile] = solution;
            Environment.CurrentDirectory = SavedCurrentDirectory[SavedCurrentDirectory.Count -1];
            SavedCurrentDirectory.RemoveAt(SavedCurrentDirectory.Count -1);
        }

        public static void Main(string[] args)
        {
            BeanTypeIdDuplicateChecker.Add(Transaction.EmptyBean.TYPEID);

            List<string> xmlFileList = new List<string>();
            var BeautifulVariableId = false;

            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-debug":
                        Debug = true;
                        break;
                    case "-DeleteOldFile":
                        DeleteOldFile = bool.Parse(args[++i]);
                        break;

                    case "-BeautifulVariableId":
                        BeautifulVariableId = true;
                        break;

                    default:
                        xmlFileList.Add(args[i]);
                        break;
                }
            }
            if (xmlFileList.Count == 0)
            {
                string xmlDefault = "solution.xml";
                if (File.Exists(xmlDefault))
                {
                    xmlFileList.Add(xmlDefault);
                }
                else
                {
                    Print(xmlDefault + " not found", ConsoleColor.Red);
                    return;
                }
            }
            foreach (string file in xmlFileList)
            {
                ImportSolution(file);
            }

            GenDerives();
            foreach (Solution sol in Solutions.Values) // compile all .包含从文件中 import 进来的。
            {
                sol.Compile();
            }

            foreach (Solution sol in Solutions.Values)
                CollectDataBeans(sol);

            foreach (string file in xmlFileList) // make 参数指定的 Solution
            {
                if (Solutions.TryGetValue(Path.GetFullPath(file), out var sol))
                {
                    if (xmlFileList.Count > 1 || Debug)
                        Print("   Make Solution: " + file, ConsoleColor.DarkCyan);
                    if (BeautifulVariableId)
                    {
                        // 编译对这个操作用户不大，需要编译是为了限制，不去修改定义有问题的配置。
                        var doc = new XmlDocument
                        {
                            PreserveWhitespace = true
                        };
                        doc.Load(file);
                        Solution.BeautifulVariableId(doc.DocumentElement);
                        using var sw = new StreamWriter(file, false, new UTF8Encoding(false));
                        doc.Save(sw);
                    }
                    else
                    {
                        sol.Make();
                    }
                }
            }

            if (DeleteOldFile)
                DeleteOldFileInGenDirs();
        }

        private static void CollectDataBeans(ModuleSpace space)
        {
            var parent = Debug ? space.Path() : null;
            foreach (var bean in space.Beans.Values)
            {
                if (bean.UseData)
                    bean.Depends(DataBeans, parent);
            }
            foreach (var protocol in space.Protocols.Values)
            {
                if (protocol.UseData)
                    protocol.Depends(DataBeans, parent);
            }
            foreach (var module in space.Modules.Values)
                CollectDataBeans(module);
        }

        public static bool isData(Types.Type type)
        {
            return DataBeans.Contains(type);
        }

        public static bool isOnlyData(Types.Type type)
        {
            return type is Bean && ((Bean)type).OnlyData;
        }

        public static void Print(object obj, ConsoleColor color)
        {
            try
            {
                Console.ForegroundColor = color;
                Console.BackgroundColor = ConsoleColor.Black;
                Console.WriteLine(obj);
            }
            finally
            {
                Console.ResetColor();
            }
        }

        public static List<Module> CompileModuleRef(ICollection<string> fullNames, string context)
        {
            List<Module> result = new List<Module>();
            foreach (string fullName in fullNames)
            {
                var module = GetNamedObject<Module>(fullName);
                if (result.Contains(module))
                    throw new Exception("CompileModuleRef duplicate module" + context);
                result.Add(module);
            }
            return result;
        }

        public static List<Protocol> CompileProtocolRef(ICollection<string> fullNames)
        {
            List<Protocol> result = new List<Protocol>();
            foreach (string fullName in fullNames)
                result.Add(GetNamedObject<Protocol>(fullName));
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

        public static List<string> Refs(XmlElement self, string nodeName, string refName)
        {
            var refs = new List<string>();
            XmlNodeList childNodes = self.ChildNodes;
            foreach (XmlNode node in childNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                XmlElement e = (XmlElement)node;
                if (e.Name.Equals(nodeName))
                {
                    var attr = e.GetAttribute(refName);
                    // 由于名字有局部名字和全名区别，这里唯一判断没有意义。
                    /*
                    if (refs.Contains(attr))
                    {
                        throw new Exception("duplicate ref name " + attr);
                    }
                    */
                    refs.Add(attr);
                }
            }
            return refs;
        }

        public static ICollection<string> Refs(XmlElement self, string nodeName)
        {
            return Refs(self, nodeName, "ref");
        }

        public static readonly int HandleServerFlag = 1;
        public static readonly int HandleClientFlag = 2;
        public static readonly int HandleScriptServerFlag = 8;
        public static readonly int HandleScriptClientFlag = 16;
        public static readonly int HandleServletFlag = 32;
        public static readonly int HandleCSharpFlags = HandleServerFlag | HandleClientFlag; // 底层语言。如果c++需要生成协议之类的，也是用这个。
        public static readonly int HandleScriptFlags = HandleScriptServerFlag | HandleScriptClientFlag;

        public static int ToHandleFlags(string handle, string name)
        {
            int f = 0;
            string hs = handle.Trim().ToLower();
            foreach (string h in hs.Split(','))
            {
                switch (h.Trim())
                {
                    case "server":
                        f |= HandleServerFlag;
                        break;
                    case "client":
                        f |= HandleClientFlag;
                        break;
                    case "serverscript":
                        f |= HandleScriptServerFlag;
                        break;
                    case "clientscript":
                        f |= HandleScriptClientFlag;
                        break;
                    case "servlet":
                        f |= HandleServletFlag;
                        break;
                    default: throw new Exception("unknown handle: '" + handle + "' for " + name);
                }
            }
            return f;
        }

        public static StreamWriter OpenWriterNoPath(string baseDir, string fileName, bool overwrite = true)
        {
            baseDir = Path.GetFullPath(baseDir);
            FileSystem.CreateDirectory(baseDir);
            string fullFileName = Path.Combine(baseDir, fileName);
            bool exists = File.Exists(fullFileName);
            if (!exists || overwrite)
            {
                //Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
                return OpenStreamWriter(fullFileName);
            }
            //Program.Print("file skip '" + fullFileName + "'");
            Outputs.TryAdd(fullFileName, null);
            return null;
        }

        private static Dictionary<string, StreamWriterOverwriteWhenChange> Outputs { get; } = new();
        private static HashSet<string> GenDirs { get; } = new();
        private static HashSet<string> OutputsAll { get; } = new();

        public static StreamWriter OpenStreamWriter(string file, bool overwrite = false)
        {
            var fullPath = Path.GetFullPath(file);
            if (!overwrite && OutputsAll.Contains(fullPath))
            {
                if (Debug)
                    Print("Skip: " + fullPath, ConsoleColor.Gray);
                return null;
            }
            if (Outputs.TryGetValue(fullPath, out var oldWriter) && oldWriter != null)
            {
                if (!overwrite)
                {
                    if (Debug)
                        Print("Skip: " + fullPath, ConsoleColor.Gray);
                    return null;
                }
                oldWriter.Close();
            }
            var sw = new StreamWriterOverwriteWhenChange(fullPath);
            Outputs[fullPath] = sw;
            return sw;
        }

        public static void AddGenDir(string dir)
        {
            var full = Path.GetFullPath(dir);
            // gen 目录也是源码，都会加入Project，即使完全没有输出，也应该存在。
            FileSystem.CreateDirectory(full);
            if (false == FileSystem.IsDirectory(full))
                throw new Exception($"{dir} Is Not A Directory.");
            GenDirs.Add(full);
        }

        public static void FlushOutputs()
        {
            foreach (var output in Outputs)
            {
                output.Value?.Dispose();
            }
            OutputsAll.UnionWith(Outputs.Keys);
            Outputs.Clear();
        }

        private static void DeleteOldFileInGenDirs()
        {
            foreach (var dir in GenDirs)
            {
                DeleteOldFileInGenDir(dir);
            }
        }

        private static void DeleteOldFileInGenDir(string dir)
        {
            foreach (var subDir in Directory.GetDirectories(dir))
            {
                DeleteOldFileInGenDir(subDir);
                if (Directory.GetFiles(subDir).Length == 0
                    && Directory.GetDirectories(subDir).Length == 0)
                {
                    // delete empty dir.
                    Directory.Delete(subDir);
                    Print($"Delete Empty Dir: {subDir}", ConsoleColor.Red);
                }
            }
            foreach (var file in Directory.GetFiles(dir))
            {
                if (OutputsAll.Contains(file) || file.EndsWith(".meta") || file.EndsWith(".pyc")) // Unity会自动生成.meta文件
                    continue;
                File.Delete(file);
                Print($"     Delete File: {file}", ConsoleColor.Red);
            }
        }

        public static string ToPinyin(string text)
        {
            StringBuilder sb = new StringBuilder();
            foreach (var c in text)
            {
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_'))
                {
                    sb.Append(c);
                    continue;
                }
                string py = NPinyin.Pinyin.GetPinyin(c);
                if (py.Length > 0)
                {
                    sb.Append(Upper1(py));
                }
            }
            return sb.ToString();
        }

        public static string FullModuleNameToFullClassName(string name)
        {
            var index = name.LastIndexOf('.');
            if (index == -1)
                return name + ".Module" + name;
            string lastname = name.Substring(index + 1);
            return name + ".Module" + lastname;
        }
    }
}
