﻿using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.javadata
{
    public class Define : Visitor
    {
        readonly string varname;
        readonly StreamWriter sw;
        readonly string prefix;

        public Define(string varname, StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.sw = sw;
            this.prefix = prefix;
        }

        void DefineNew(Type type)
        {
            string tName = TypeName.GetName(type);
            sw.WriteLine(prefix + "var " + varname + " = new " + tName + "();");
        }

        void DefineStack(Type type)
        {
            string tName = TypeName.GetName(type);
            sw.WriteLine(prefix + tName + " " + varname + ";");
        }

        public void Visit(TypeBool type)
        {
            DefineStack(type);
        }

        public void Visit(TypeByte type)
        {
            DefineStack(type);
        }

        public void Visit(TypeShort type)
        {
            DefineStack(type);
        }

        public void Visit(TypeInt type)
        {
            DefineStack(type);
        }

        public void Visit(TypeLong type)
        {
            DefineStack(type);
        }

        public void Visit(TypeFloat type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDouble type)
        {
            DefineStack(type);
        }

        public void Visit(TypeBinary type)
        {
            DefineStack(type);
        }

        public void Visit(TypeString type)
        {
            DefineStack(type);
        }

        public void Visit(TypeList type)
        {
            DefineNew(type);
        }

        public void Visit(TypeSet type)
        {
            DefineNew(type);
        }

        public void Visit(TypeMap type)
        {
            DefineNew(type);
        }

        public void Visit(Bean type)
        {
            DefineNew(type);
        }

        public void Visit(BeanKey type)
        {
            DefineNew(type);
        }

        public void Visit(TypeDynamic type)
        {
            sw.WriteLine($"{prefix}var {varname} = new DynamicData_{type.Variable.Name}();");
        }

        public void Visit(TypeQuaternion type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector2 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector2Int type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector3 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector3Int type)
        {
            DefineStack(type);
        }

        public void Visit(TypeVector4 type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDecimal type)
        {
            DefineStack(type);
        }
    }
}
