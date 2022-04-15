using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Gen
{
    public class GenAction
    {
        public ParameterInfo Parameter { get; }

        public Type[] GenericArguments { get; }
        public string[] GenericArgumentVarNames { get; }

        public GenAction(ParameterInfo p)
        {
            if (false == Gen.IsActionDelegate(p.ParameterType))
                throw new Exception("Need A Action Callback.");

            Parameter = p;

            GenericArguments = Parameter.ParameterType.GetGenericArguments();
            GenericArgumentVarNames = new string[GenericArguments.Length];
            for (int i = 0; i < GenericArguments.Length; ++i)
            {
                var arg = GenericArguments[i];

                if (Gen.IsDelegate(arg))
                    throw new Exception("Action GenericArgument IsDelegate.");

                // 这个好像不可能，判断一下吧。
                if (arg.IsByRef)
                    throw new Exception("Action GenericArgument IsByRef.");

                GenericArgumentVarNames[i] = "tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet();
            }
        }

        public string GetGenericArgumentsDefine()
        {
            return Gen.Instance.GetTypeName(Parameter.ParameterType);
            /*
            StringBuilder sb = new StringBuilder();
            sb.Append("<");
            for (int i = 0; i < GenericArguments.Length; ++i)
            {
                var argType = GenericArguments[i];
                if (i > 0)
                    sb.Append(", ");
                sb.Append(Gen.Instance.GetTypeName(argType));
            }
            sb.Append(">");

            return sb.ToString();
            */
        }

        private string GetGenericArgumentVarNamesDefine(int offset = 0)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < GenericArgumentVarNames.Length; ++i)
            {
                if (i > offset)
                    sb.Append(", ");
                sb.Append(GenericArgumentVarNames[i]);
            }
            return sb.ToString();
        }

        public static GenAction CreateIf(ParameterInfo p)
        {
            var ParameterType = p.ParameterType;
            if (Gen.IsDelegate(ParameterType))
                return new GenAction(p);
            return null;
        }

        public void Verify(MethodOverride m)
        {
            switch (m.OverrideType)
            {
                case OverrideType.RedirectHash:
                case OverrideType.RedirectToServer:
                    break;

                case OverrideType.RedirectAll:
                    if (GenericArguments.Length != 1)
                        throw new Exception(m.Method.Name + ": RedirectAll Result Handle Too Many Parameters.");
                    //if (!RedirectResult.isAssignableFrom(m.ResultType))
                    //    throw new Exception(m.method.getName() + ": RedirectAll Result Type Must Extend RedirectContext");
                    break;
            }
        }

        public string GetCallString(List<string> vars)
        {
            var sb = new StringBuilder();
            for (int i = 0; i < vars.Count; ++i)
            {
                if (i > 0)
                    sb.Append(", ");
                sb.Append(vars[i]);
            }
            return sb.ToString();
        }

        public void GenDecodeAndCallback(string prefix, StringBuilder sb, MethodOverride m)
        {
            GenDecodeAndCallback("App.Zeze", prefix, sb, Parameter.Name, m);
        }

        public void GenDecodeAndCallback(string zzName, string prefix, StringBuilder sb, string actName, MethodOverride m)
        {
            var resultVarNames = new List<string>();
            for (int i = 0; i < m.ResultHandle.GenericArguments.Length; ++i) {
                resultVarNames.Add("tmp" + Gen.Instance.TmpVarNameId.IncrementAndGet());
                var rClass = m.ResultHandle.GenericArguments[i];
                Gen.Instance.GenLocalVariable(sb, prefix, rClass, resultVarNames[i]);
                Gen.Instance.GenDecode(sb, prefix, rClass, resultVarNames[i]);
            }
            switch (m.TransactionLevel)
            {
                case Zeze.Transaction.TransactionLevel.Serializable:
                case Zeze.Transaction.TransactionLevel.AllowDirtyWhenAllRead:
                    sb.AppendLine($"{prefix}await {zzName}.NewProcedure(async () => {{ {actName}({GetCallString(resultVarNames)}); return 0L; }}, \"ModuleRedirectResponse Procedure\").CallAsync();");
                    break;

                default:
                    sb.AppendLine($"{prefix}{actName}({GetCallString(resultVarNames)});");
                    break;
            }
        }

        public void GenEncode(List<string> resultVarNames, string prefix, StringBuilder sb, MethodOverride m)
        {
            for (int i = 0; i < m.ResultHandle.GenericArguments.Length; ++i)
            {
                var rClass = m.ResultHandle.GenericArguments[i];
                Gen.Instance.GenEncode(sb, prefix, rClass, resultVarNames[i]);
            }
        }
    }
}
