﻿using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Define : Types.Visitor
    {
        private string varname;
        private System.IO.StreamWriter sw;
        private string prefix;

        public Define(string varname, System.IO.StreamWriter sw, string prefix)
        {
            this.varname = varname;
            this.sw = sw;
            this.prefix = prefix;
        }

        private void DefineNew(Types.Type type)
        {
            string tName = TypeName.GetName(type);
            sw.WriteLine(prefix + tName + " " + varname + " = new " + tName + "();");
        }

        private void DefineStack(Types.Type type)
        {
            string typeName = TypeName.GetName(type);
            sw.WriteLine(prefix + typeName + " " + varname + ";");
        }

        public void Visit(Bean type)
        {
            DefineNew(type);
        }

        public void Visit(BeanKey type)
        {
            DefineNew(type);
        }

        public void Visit(TypeByte type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDouble type)
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

        public void Visit(TypeBool type)
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

        public void Visit(TypeFloat type)
        {
            DefineStack(type);
        }

        public void Visit(TypeShort type)
        {
            DefineStack(type);
        }

        public void Visit(TypeDynamic type)
        {
            DefineStack(type);
        }
    }
}
