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
        public Type ActionType { get; }

        public Type[] GenericArguments { get; }
        public string[] GenericArgumentVarNames { get; }
        public string VarName { get; }

        public GenAction(Type actionType, string varName)
        {
            if (false == Gen.IsActionDelegate(actionType))
                throw new Exception("Need A Action Callback.");

            ActionType = actionType;
            VarName = varName;

            GenericArguments = ActionType.GetGenericArguments();
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
            if (GenericArguments.Length == 0)
                return string.Empty;

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

        public void GenActionEncode(StringBuilder sb, string prefix)
        {
            sb.AppendLine($"{prefix}System.Action{GetGenericArgumentsDefine()} {VarName} = ({GetGenericArgumentVarNamesDefine()}) =>");
            sb.AppendLine($"{prefix}{{");
            if (GenericArguments.Length > 0)
            {
                sb.AppendLine($"{prefix}    var _bb_ = Zeze.Serialize.ByteBuffer.Allocate();");
                GenEncode(sb, prefix + "    ");
                sb.AppendLine($"{prefix}    _actions_.Add(new Zezex.Provider.BActionParam() {{ Name = \"{VarName}\", Params = new Zeze.Net.Binary(_bb_) }});");
            }
            sb.AppendLine($"{prefix}}};");
        }

        public void GenActionDecode(StringBuilder sb, string prefix, string callcontext = "", int offset = 0)
        {
            sb.AppendLine($"{prefix}System.Action<Zeze.Net.Binary> _{VarName}_ = (_params_) =>");
            sb.AppendLine($"{prefix}{{");
            if (GenericArguments.Length > 0)
            {
                sb.AppendLine($"{prefix}    var _bb_ = Zeze.Serialize.ByteBuffer.Wrap(_params_);");
                GenDecode(sb, prefix + "    ", offset);
                string sep = string.IsNullOrEmpty(callcontext) ? "" : ", ";
                sb.AppendLine($"{prefix}    {VarName}({callcontext}{sep}{GetGenericArgumentVarNamesDefine(offset)});");
            }
            sb.AppendLine($"{prefix}}};");
        }

        private void GenEncode(StringBuilder sb, string prefix)
        {
            for (int i = 0; i < GenericArguments.Length; ++i)
            {
                var type = GenericArguments[i];
                Gen.Instance.GenEncode(sb, prefix, type, GenericArgumentVarNames[i]);
            }
        }

        private void GenDecode(StringBuilder sb, string prefix, int offset = 0)
        {
            for (int i = offset; i < GenericArguments.Length; ++i)
            {
                var type = GenericArguments[i];
                Gen.Instance.GenLocalVariable(sb, prefix, type, GenericArgumentVarNames[i]);
                Gen.Instance.GenDecode(sb, prefix, type, GenericArgumentVarNames[i]);
            }
        }

        public bool IsOnHashEnd => Gen.IsOnHashEnd(GenericArguments);

        public static List<GenAction> GetActions(List<ParameterInfo> parameters)
        {
            var result = new List<GenAction>();
            for (int i = 0; i < parameters.Count; ++i)
            {
                var p = parameters[i];
                if (Gen.IsDelegate(p.ParameterType))
                {
                    result.Add(new GenAction(p.ParameterType, p.Name));
                }
            }
            return result;
        }

    }
}
