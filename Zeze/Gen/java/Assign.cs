﻿using System;
using System.IO;
using Zeze.Gen.Types;

namespace Zeze.Gen.java
{
    public class Assign : Visitor
    {
        private StreamWriter sw;
        private Variable var;
        private string prefix;

        public static void Make(Bean bean, StreamWriter sw, string prefix)
        {
            sw.WriteLine(prefix + "public void Assign(" + bean.Name + " other) {");
            foreach (Variable var in bean.Variables)
                var.VariableType.Accept(new Assign(var, sw, prefix + "    "));
            sw.WriteLine(prefix + "}");
            sw.WriteLine();
        }

        public Assign(Variable var, StreamWriter sw, string prefix)
        {
            this.var = var;
            this.sw = sw;
            this.prefix = prefix;
        }

        void Visitor.Visit(Bean type)
        {
            sw.WriteLine(prefix + var.Getter + ".Assign(other." + var.Getter + ");");
        }

        void Visitor.Visit(BeanKey type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeByte type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeDouble type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeInt type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeLong type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeBool type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeBinary type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeString type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeList type)
        {
            sw.WriteLine(prefix + var.Getter + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e";

            sw.WriteLine(prefix + "for (var e : other." + var.Getter +")");
            sw.WriteLine(prefix + "    " + var.Getter + ".add(" + copyif + ");");
        }

        void Visitor.Visit(TypeSet type)
        {
            sw.WriteLine(prefix + var.Getter + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.Copy()" : "e"; // set 里面现在不让放 bean，先这样写吧。

            sw.WriteLine(prefix + "for (var e : other." + var.Getter + ")");
            sw.WriteLine(prefix + "    " + var.Getter + ".add(" + copyif + ");");
        }

        void Visitor.Visit(TypeMap type)
        {
            sw.WriteLine(prefix + var.Getter + ".clear();");
            string copyif = type.ValueType.IsNormalBean ? "e.getValue().Copy()" : "e.getValue()";

            sw.WriteLine(prefix + "for (var e : other." + var.Getter + ".entrySet())");
            sw.WriteLine(prefix + "    " + var.Getter + ".put(e.getKey(), " + copyif + ");");
        }

        void Visitor.Visit(TypeFloat type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeShort type)
        {
            sw.WriteLine(prefix + var.Setter($"other.{var.Getter}") + ";");
        }

        void Visitor.Visit(TypeDynamic type)
        {
            sw.WriteLine(prefix + var.Getter + ".Assign(other." + var.Getter + ");");
        }
    }
}
