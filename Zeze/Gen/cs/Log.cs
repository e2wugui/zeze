using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.cs
{
    public class Log : Visitor
    {
        readonly StreamWriter sw;
        readonly Bean bean;
        readonly Variable var;
        readonly string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            foreach (Variable var in bean.Variables)
            {
                if (bean.Version.Equals(var.Name))
                    continue; // 版本变量不需要生成Log实现。
                var.VariableType.Accept(new Log(bean, sw, var, prefix));
                sw.WriteLine();
            }
        }

        public Log(Bean bean, StreamWriter sw, Variable var, string prefix)
        {
            this.bean = bean;
            this.sw = sw;
            this.var = var;
            this.prefix = prefix;
        }

        public void Visit(Bean type)
        {
        }

        void WriteLogValue(Type type)
        {
            string valueName = TypeName.GetName(type);
            sw.WriteLine(prefix + "sealed class Log_" + var.NamePrivate + " : Zeze.Transaction.Log<" + valueName + ">");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + $"    public override void Commit() {{ (({bean.Name})Belong).{var.NamePrivate} = this.Value; }}");
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

        void WriteCollectionLog(Type type)
        {
            /*
            var tn = new TypeName();
            type.Accept(tn);

            sw.WriteLine(prefix + "sealed class Log_" + var.NamePrivate + " : " + tn.name + ".LogV");
            sw.WriteLine(prefix + "{");
            sw.WriteLine(prefix + "    public Log_" + var.NamePrivate + "(" + bean.Name + " host, " + tn.nameCollectionImplement + " value) : base(host, value) {}");
            sw.WriteLine(prefix + "    public override long LogKey => Bean.ObjectId + " + var.Id + ";");
            sw.WriteLine(prefix + "    public " + bean.Name + " BeanTyped => (" + bean.Name + ")Bean;");
            sw.WriteLine(prefix + "    public override void Commit() { Commit(BeanTyped." + var.NamePrivate + "); }");
            sw.WriteLine(prefix + "}");
            */
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
            // TypeDynamic 使用写好的类 Zeze.Transaction.DynamicBean，
            // 不再需要生成Log。在这里生成 DynamicBean 需要的两个方法。
        }

        public void Visit(TypeQuaternion type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeVector2 type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeVector2Int type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeVector3 type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeVector3Int type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeVector4 type)
        {
            WriteLogValue(type);
        }

        public void Visit(TypeDecimal type)
        {
            WriteLogValue(type);
        }
    }
}
