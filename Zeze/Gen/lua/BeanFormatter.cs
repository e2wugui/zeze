using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.lua
{
    public class BeanFormatter
    {
        public static void Make(string moduleName, string beanName, long beanTypeId, List<Types.Variable> vars, System.IO.StreamWriter sw)
        {
            sw.WriteLine($"{moduleName}.{beanName} = {{");
            sw.WriteLine($"    _TypeId_ = {beanTypeId},");
            foreach (var v in vars)
            {
                sw.WriteLine($"    {v.Name} = {v.Id},");
            }
            sw.WriteLine("}");
        }

        public static void MakeMeta(long typeId, List<Types.Variable> vars, System.IO.StreamWriter sw)
        {
            sw.WriteLine("meta.beans[" + typeId + "] = {");
            foreach (var v in vars)
                sw.WriteLine($"    [{v.Id}] = {TypeMeta.Get(v.VariableType)},");
            sw.WriteLine("}");
        }
    }
}
