using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Property : Types.Visitor
    {
        System.IO.StreamWriter sw;
        Types.Variable var;
        string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Property(sw, var, prefix));
            }
        }

        public Property(System.IO.StreamWriter sw, Types.Variable var, string prefix)
        {
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
        }

        private void WriteProperty(Types.Type type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(this.ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(BeanKey type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeByte type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeInt type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeLong type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBool type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeBinary type)
        {
            // private
            sw.WriteLine(prefix + "private " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(this.ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + "Copy");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        return Helper.Copy(" + var.NameUpper1 + ");");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        " + var.NameUpper1 + " = Helper.Copy(value);");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.NameUpper1 + "Encode(Serializable _s_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    " + var.NameUpper1 + " = Helper.Encode(_s_).Copy();");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
            sw.WriteLine(prefix + "public void " + var.NameUpper1 + "Decode(Serializable _s_)");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    ByteBuffer _bb_ = ByteBuffer.Wrap(" + var.NameUpper1 + ");");
            sw.WriteLine(prefix + "    _s_.Decode(_bb_);");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public void Visit(TypeString type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeList type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            sw.WriteLine();
        }

        public void Visit(TypeSet type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            sw.WriteLine();
        }

        public void Visit(TypeMap type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1 + " => " + var.NamePrivate + ";");
            sw.WriteLine();
        }

        public void Visit(TypeFloat type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeShort type)
        {
            WriteProperty(type);
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + "public " + TypeName.GetName(type) + " " + var.NameUpper1);
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    get");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "            return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        if (txn == null) return " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "        var log = (Log_" + var.NamePrivate + ")txn.GetLog(this.ObjectId + " + var.Id + ");");
            sw.WriteLine(prefix + "        return log != null ? log.Value : " + var.NamePrivate + ";");
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "    set");
            sw.WriteLine(prefix + "    {");
            sw.WriteLine(prefix + "        if (null != value)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            switch (value.GetType().FullName)");
            sw.WriteLine(prefix + "            {");
            foreach (Bean bean in type.RealBeans)
            {
                sw.WriteLine($"{prefix}                case \"{TypeName.GetName(bean)}\": break;");
            }
            sw.WriteLine(prefix + "                default: throw new System.Exception(\"Is Not Supported Dynamic Bean\");");
            sw.WriteLine(prefix + "            }");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        if (false == this.IsManaged)");
            sw.WriteLine(prefix + "        {");
            sw.WriteLine(prefix + "            " + var.NamePrivate + " = value;");
            sw.WriteLine(prefix + "            return;");
            sw.WriteLine(prefix + "        }");
            sw.WriteLine(prefix + "        value?.InitTableKey(TableKey);");
            sw.WriteLine(prefix + "        var txn = Zeze.Transaction.Transaction.Current;");
            sw.WriteLine(prefix + "        txn.PutLog(new Log_" + var.NamePrivate + "(this, value));"); // 
            sw.WriteLine(prefix + "    }");
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }
    }
}
