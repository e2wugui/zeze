using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ConfigEditor.Gen.cs
{
    public class Main
    {
        public static void Gen(FormMain main, Property.DataOutputFlags flags, FormBuildProgress progress)
        {
            switch (flags)
            {
                case Property.DataOutputFlags.Server:
                    main.Documents.ForEachFile((Documents.File file) =>
                    {
                        progress.AppendLine($"生成cs服务器代码. {file.Document.RelateName}", Color.Black);
                        BeanFormatter.Gen(main.ConfigProject.ServerSrcDirectory, file.Document, Property.DataOutputFlags.Server);
                        return progress.Running;
                    });
                    if (false == progress.Running)
                        return;
                    GenManager(main, main.ConfigProject.ServerSrcDirectory);
                    break;

                case Property.DataOutputFlags.Client:
                    main.Documents.ForEachFile((Documents.File file) =>
                    {
                        progress.AppendLine($"生成cs客户端代码. {file.Document.RelateName}", Color.Black);
                        BeanFormatter.Gen(main.ConfigProject.ClientSrcDirectory, file.Document, Property.DataOutputFlags.Client);
                        return progress.Running;
                    });
                    if (false == progress.Running)
                        return;
                    GenManager(main, main.ConfigProject.ClientSrcDirectory);
                    break;
            }

        }

        private static void GenManager(FormMain main, string srcdir)
        {
            string dir = System.IO.Path.Combine(srcdir, Document.NamespacePrefix);
            System.IO.Directory.CreateDirectory(dir);
            string file = System.IO.Path.Combine(dir, "Manager.cs");
            using (System.IO.StreamWriter sw = new System.IO.StreamWriter(file, false, Encoding.UTF8))
            {
                sw.WriteLine("// auto generate");
                sw.WriteLine();
                sw.WriteLine($"using System;");
                sw.WriteLine($"using System.Collections.Generic;");
                sw.WriteLine($"using System.Xml;");
                sw.WriteLine($"using System.IO;");
                sw.WriteLine();
                sw.WriteLine("namespace Config");
                sw.WriteLine("{");
                sw.WriteLine("    public class Manager");
                sw.WriteLine("    {");

                if (false == main.PropertyManager.Properties.TryGetValue(Property.Id.PName, out var pid))
                    throw new Exception("Property.Id miss!");

                main.Documents.ForEachFile((Documents.File fileForEach) =>
                {
                    var doc = fileForEach.Document;
                    string varName = doc.RelateName.Replace('.', '_');
                    sw.WriteLine($"        public static List<{doc.RelateName}> {varName} {{ get; }} = new List<{doc.RelateName}>();");

                    foreach (var var in doc.BeanDefine.Variables)
                    {
                        if (false == var.IsKeyable())
                            continue;

                        if (false == var.PropertiesList.Contains(pid))
                            continue;

                        sw.WriteLine($"        public static Dictionary<{TypeHelper.GetName(var)}, {doc.RelateName}> {varName}Map{var.Name} {{ get; }} = new Dictionary<{TypeHelper.GetName(var)}, {doc.RelateName}>();");
                    }
                    return true;
                });
                sw.WriteLine();
                sw.WriteLine("        public static void Load(string home)");
                sw.WriteLine("        {");
                main.Documents.ForEachFile((Documents.File fileForEach) =>
                {
                    var doc = fileForEach.Document;
                    string varName = doc.RelateName.Replace('.', '_');
                    sw.WriteLine($"            {varName}.Clear();");
                    sw.WriteLine($"            Load(home, \"{doc.RelateName}\", (XmlElement e) => ");
                    sw.WriteLine($"            {{");
                    sw.WriteLine($"                var bean = new {doc.RelateName}(e);");
                    sw.WriteLine($"                {varName}.Add(bean);");
                    foreach (var var in doc.BeanDefine.Variables)
                    {
                        if (var.Type == VarDefine.EType.List)
                            continue;

                        if (false == var.PropertiesList.Contains(pid))
                            continue;

                        sw.WriteLine($"                {varName}Map{var.Name}.Add(bean.V{var.Name}, bean);");
                    }
                    sw.WriteLine($"            }});");
                    return true;
                });
                sw.WriteLine("        }");
                sw.WriteLine();
                sw.WriteLine("        public static void Load(string home, string relate, Action<XmlElement> action)");
                sw.WriteLine("        {");
                sw.WriteLine("            string fileName = Path.Combine(home, relate.Replace('.', Path.DirectorySeparatorChar));");
                sw.WriteLine("            XmlDocument Xml = new XmlDocument();");
                sw.WriteLine("            Xml.Load(fileName);");
                sw.WriteLine("            XmlElement self = Xml.DocumentElement;");
                sw.WriteLine("            if (false == self.Name.Equals(\"ZezeConfig\"))");
                sw.WriteLine("                throw new Exception(\" is not ZezeConfig\");");
                sw.WriteLine("            XmlNodeList childNodes = self.ChildNodes;");
                sw.WriteLine("            foreach (XmlNode node in childNodes)");
                sw.WriteLine("            {");
                sw.WriteLine("                if (XmlNodeType.Element != node.NodeType)");
                sw.WriteLine("                    continue;");
                sw.WriteLine();
                sw.WriteLine("                XmlElement e = (XmlElement)node;");
                sw.WriteLine("                switch (e.Name)");
                sw.WriteLine("                {");
                sw.WriteLine("                    case \"bean\":");
                sw.WriteLine("                        action(e);");
                sw.WriteLine("                        break;");
                sw.WriteLine("                    default:");
                sw.WriteLine("                        throw new Exception(\"Unknown Element Name \" + e.Name);");
                sw.WriteLine("                }");
                sw.WriteLine("            }");
                sw.WriteLine("        }");
                sw.WriteLine();
                sw.WriteLine("    public static void LoadList(XmlElement self, Action<XmlElement> action)");
                sw.WriteLine("        {");
                sw.WriteLine("            foreach (XmlNode nodeList in self.ChildNodes)");
                sw.WriteLine("            {");
                sw.WriteLine("                if (XmlNodeType.Element != nodeList.NodeType)");
                sw.WriteLine("                    continue;");
                sw.WriteLine("                XmlElement eList = (XmlElement)nodeList;");
                sw.WriteLine("                switch (eList.Name)");
                sw.WriteLine("                {");
                sw.WriteLine("                    case \"list\":");
                sw.WriteLine("                        foreach (XmlNode bInList in eList.ChildNodes)");
                sw.WriteLine("                        {");
                sw.WriteLine("                            if (XmlNodeType.Element != bInList.NodeType)");
                sw.WriteLine("                                continue;");
                sw.WriteLine("                            XmlElement eInList = (XmlElement)bInList;");
                sw.WriteLine("                            if (!eInList.Name.Equals(\"bean\"))");
                sw.WriteLine("                                throw new Exception(\"Unknown Element In List\");");
                sw.WriteLine("                            action(eInList);");
                sw.WriteLine("                        }");
                sw.WriteLine("                        break;");
                sw.WriteLine("                    default:");
                sw.WriteLine("                        throw new Exception(\"Unknown Element In VarList\");");
                sw.WriteLine("                }");
                sw.WriteLine("            }");
                sw.WriteLine("        }");
                sw.WriteLine();
                sw.WriteLine("    }");
                sw.WriteLine("}");
            }
        }
    }
}
