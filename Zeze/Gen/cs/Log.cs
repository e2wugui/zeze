using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Log : Types.Visitor
    {
        System.IO.StreamWriter sw;
        Types.Bean bean;
        Types.Variable var;
        string prefix;

        public static void Make(Types.Bean bean, System.IO.StreamWriter sw, string prefix)
        {
            foreach (Types.Variable var in bean.Variables)
            {
                var.VariableType.Accept(new Log(bean, sw, var, prefix));
                sw.WriteLine("");
            }
        }

        public Log(Types.Bean bean, System.IO.StreamWriter sw, Types.Variable var, string prefix)
        {
            this.bean = bean;
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
        }

        private void WriteLogValue(Types.Type type)
        {
            string valueName = TypeName.GetName(type);
            sw.WriteLine(prefix + "private sealed class Log_" + var.NamePrivate + " : Zeze.Transaction.Log<" + bean.Name + ", " + valueName + ">");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " self, " + valueName + " value) : base(self, value) { }");
            sw.WriteLine(prefix + "    public override long LogKey => this.Bean.ObjectId + " + var.Id + ";");
            sw.WriteLine(prefix + "    public override void Commit() { this.BeanTyped." + var.NamePrivate + " = this.Value; }");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(BeanKey type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeByte type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDouble type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeInt type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeLong type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeBool type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeBinary type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeString type)
        {
            WriteLogValue(type);
        }

        private void WriteCollectionLog(Types.Type type)
        {
            var tn = new TypeName();
            type.Accept(tn);

            sw.WriteLine(prefix + "private sealed class Log_" + var.NamePrivate + " : " + tn.name + ".LogV");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " host, " + tn.nameCollectionImplement + " value) : base(host, value) { }");
            sw.WriteLine(prefix + "    public override long LogKey => Bean.ObjectId + " + var.Id + ";");
            sw.WriteLine(prefix + "    public " + bean.Name + " BeanTyped => (" + bean.Name + ")Bean;");
            sw.WriteLine(prefix + "    public override void Commit() { Commit(BeanTyped." + var.NamePrivate + "); }");
            sw.WriteLine(prefix + "}");
        }

        public void Visit(TypeList type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeSet type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeMap type)
        {
            WriteCollectionLog(type);
        }

        public void Visit(TypeFloat type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeShort type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDynamic type)
        {
            WriteLogValue(type);
        }
    }
}
