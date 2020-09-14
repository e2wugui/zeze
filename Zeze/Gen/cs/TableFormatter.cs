using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;

namespace Zeze.Gen.cs
{
    public class TableFormatter
    {
        Table table;
        string genDir;

        public TableFormatter(Table table, string genDir)
        {
            this.table = table;
            this.genDir = genDir;
        }

        public void Make()
        {
            using System.IO.StreamWriter sw = table.Space.OpenWriter(genDir, table.Name + ".cs");

            sw.WriteLine("// auto-generated");
            sw.WriteLine("");
            sw.WriteLine("using Zeze.Serialize;");
            //sw.WriteLine("using Zeze.Transaction.Collections;");
            sw.WriteLine("");
            sw.WriteLine("namespace " + table.Space.Path());
            sw.WriteLine("{");
            string key = TypeName.GetName(table.KeyType);
            string value = TypeName.GetName(table.ValueType);
            sw.WriteLine("    public sealed class " + table.Name + " : Zeze.Transaction.Table<" + key + ", " + value + ">");
            sw.WriteLine("    {");
            sw.WriteLine("        public " + table.Name + "() : base(\"" + table.Space.Path("_", table.Name) + "\")");
            sw.WriteLine("        {");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override bool IsMemory => " + (table.IsMemory ? "true;" : "false;"));
            sw.WriteLine("        public override bool IsAutoKey => " + (table.IsAutoKey ? "true;" : "false;"));
            sw.WriteLine();
            if (table.IsAutoKey)
            {
                sw.WriteLine("        public long Insert(" + value + " value)");
                sw.WriteLine("        {");
                sw.WriteLine("            long key = AutoKey.Next();");
                sw.WriteLine("            Insert(key, value);");
                sw.WriteLine("            return key;");
                sw.WriteLine("        }");
                sw.WriteLine();
            }
            sw.WriteLine("        public override " + TypeName.GetName(table.KeyType) + " DecodeKey(ByteBuffer _os_)");
            sw.WriteLine("        {");
            table.KeyType.Accept(new Define("_v_", sw, "            "));
            table.KeyType.Accept(new Decode("_v_", -1, "_os_", sw, "            "));
            sw.WriteLine("            return _v_;");
            sw.WriteLine("        }");
            sw.WriteLine();
            sw.WriteLine("        public override ByteBuffer EncodeKey(" + TypeName.GetName(table.KeyType) + " _v_)");
            sw.WriteLine("        {");
            sw.WriteLine("            ByteBuffer _os_ = ByteBuffer.Allocate();");            
            table.KeyType.Accept(new Encode("_v_", -1, "_os_", sw, "            "));
            sw.WriteLine("            return _os_;");
            sw.WriteLine("        }");
            sw.WriteLine("    }");
            sw.WriteLine("}");

        }
    }
}
