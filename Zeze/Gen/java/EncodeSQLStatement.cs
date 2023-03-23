using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Game.Bag;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class EncodeSQLStatement : Visitor
    {
        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {");
            var hasParentName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new EncodeSQLStatement(v, null, v.Id, "st", sw, prefix + "    ", hasParentName));
            }
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
        }

        public static void Make(BeanKey bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "@Override");
            sw.WriteLine(prefix + "public void encodeSQLStatement(java.util.ArrayList<String> parents, Zeze.Serialize.SQLStatement st) {");
            var hasParentsName = new bool[1];
            foreach (Variable v in bean.Variables)
            {
                if (v.Transient)
                    continue;
                v.VariableType.Accept(new EncodeSQLStatement(v, null, v.Id, "st", sw, prefix + "    ", hasParentsName));
            }
            sw.WriteLine(prefix + "}");
            // sw.WriteLine();
        }

        void ensureParentsName()
        {
            if (!hasParentsName[0])
            {
                hasParentsName[0] = true;
                sw.WriteLine($"{prefix}var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);");
            }
        }

        public void Visit(TypeBool type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendBoolean(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeByte type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendByte(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeShort type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendShort(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeInt type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendInt(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeLong type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendLong(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeFloat type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendFloat(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeDouble type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendDouble(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeBinary type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendBinary(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeString type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString(_parents_name_ + \"{var.Name}\", {var.Getter});");
        }

        public void Visit(TypeList type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString(_parents_name_ + \"{var.Name}\", Zeze.Serialize.Helper.encodeJson({var.Getter}));");
        }

        public void Visit(TypeSet type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString(_parents_name_ + \"{var.Name}\", Zeze.Serialize.Helper.encodeJson({var.Getter}));");
        }

        public void Visit(TypeMap type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString(_parents_name_ + \"{var.Name}\", Zeze.Serialize.Helper.encodeJson({var.Getter}));");
        }

        public void Visit(Bean type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{var.Getter}.encodeSQLStatement(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(BeanKey type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}{var.Getter}.encodeSQLStatement(parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeDynamic type)
        {
            ensureParentsName();
            sw.WriteLine($"{prefix}st.appendString(_parents_name_ + \"{var.Name}\", Zeze.Serialize.Helper.encodeJson({var.Getter}));");
        }

        public void Visit(TypeQuaternion type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeQuaternion({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector2({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector2Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector2Int({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector3({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector3Int type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector3Int({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        public void Visit(TypeVector4 type)
        {
            sw.WriteLine($"{prefix}parents.add(\"{var.Name}\");");
            sw.WriteLine($"{prefix}Zeze.Serialize.Helper.encodeVector4({var.Getter}, parents, {bb});");
            sw.WriteLine($"{prefix}parents.remove(parents.size() - 1);");
        }

        Variable var;
        string varname;
        int id;
        string bb;
        StreamWriter sw;
        string prefix;
        bool[] hasParentsName;

        string Getter => var != null ? var.Getter : varname;

        public EncodeSQLStatement(Variable var, string varname, int id, string bb, StreamWriter sw, string prefix, bool[] hasParentsName)
        {
            this.var = var;
            this.varname = varname;
            this.id = id;
            this.bb = bb;
            this.sw = sw;
            this.prefix = prefix;
            this.hasParentsName = hasParentsName;
        }
    }
}
