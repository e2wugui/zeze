using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Gen
{
    // Mode,  Result Callback, Result await
    // Void,  Support          Not Support
    // Async, Support          Support
    // * 不再支持返回 TaskCompletionSource 的同步方式了。
    public class MethodMode
    {
        public ParameterInfo ReturnParam { get; }
        public Type ParameterType { get; }
        public bool IsAsync { get; }
        public Type ResultType { get; }
        public bool HasResult => ResultType != null;
        public MethodMode(ParameterInfo rp)
        {
            ReturnParam = rp;
            ParameterType = rp.ParameterType;

            if (ParameterType == typeof(void))
            {
                IsAsync = false;
                return;
            }
            if (ParameterType == typeof(Task))
            {
                IsAsync = true;
                return;
            }
            if (ParameterType.IsGenericType)
            {
                if (ParameterType.GetGenericTypeDefinition() == typeof(Task<>))
                {
                    var arg = ParameterType.GetGenericArguments();
                    if (arg.Length != 1)
                        throw new Exception("ReturnType Only Support One Generic Argument."); // 这个应该不可能。满检查一下吧。
                    IsAsync = true;
                    ResultType = arg[0];
                    return;
                }
            }
            throw new Exception("ReturnType Only Support Void Or Task(async)");
        }

        public string GetCallReturnName()
        {
            return (IsAsync ? "async " : "") + Gen.Instance.GetTypeName(ParameterType);
        }

        public string GetFutureDefine()
        {
            if (IsAsync && HasResult)
            {
                string fullName = "";
                fullName += "<";
                bool first = true;
                foreach (var parameter in ParameterType.GetGenericArguments())
                {
                    if (first)
                        first = false;
                    else
                        fullName += ", ";
                    fullName += Gen.Instance.GetTypeName(parameter);
                }
                fullName += ">";
                return fullName;
            }
            return "";
        }

        public void GenFutureDecodeAndSet(string prefix, StringBuilder sb, string futureName)
        {
            if (HasResult)
            {
                var returnResultVarName = "theResult" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                Gen.Instance.GenLocalVariable(sb, prefix, ResultType, returnResultVarName);
                Gen.Instance.GenDecode(sb, prefix, ResultType, returnResultVarName);
                sb.AppendLine($"{prefix}{futureName}.TrySetResult({returnResultVarName});");
            }
            else
            {
                sb.AppendLine($"{prefix}{futureName}.TrySetResult();");
            }
        }

        public void GenCallAndEncode(string prefix, StringBuilder sb, string callstr)
        {
            if (IsAsync)
            {
                if (HasResult)
                {
                    var asyncResult = "asyncResult" + Gen.Instance.TmpVarNameId.IncrementAndGet();
                    sb.AppendLine($"            var {asyncResult} = await {callstr};");
                    Gen.Instance.GenEncode(sb, prefix, ResultType, asyncResult);
                }
                else
                {
                    sb.AppendLine($"            await {callstr};");
                }
            }
            else
            {
                sb.AppendLine($"            {callstr};");
            }
        }
    }
}
