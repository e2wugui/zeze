using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zeze.Arch.Gen
{
    public class MethodOverride
    {
        public MethodInfo Method { get; }
        public OverrideType OverrideType { get; }
        public Attribute Attribute { get; }
        public GenAction ResultHandle { get; private set; }
        public MethodMode MethodMode { get; }

        public Zeze.Transaction.TransactionLevel TransactionLevel = Transaction.TransactionLevel.Serializable;

        public MethodOverride(MethodInfo method, OverrideType type, Attribute attribute)
        {
            var tLevelAnn = method.GetCustomAttribute<TransactionLevelAttribute>();
            if (tLevelAnn != null)
                TransactionLevel = tLevelAnn.Level;

            if (false == method.IsVirtual)
                throw new Exception("ModuleRedirect Need Virtual。{method}");
            Method = method;
            OverrideType = type;
            Attribute = attribute;
            MethodMode = new(method.ReturnParameter);
        }

        public ParameterInfo ParameterHashOrServer { get; private set; } // When RedirectHash RedirectToServer
        public List<ParameterInfo> ParametersNormal { get; } = new List<ParameterInfo>();

        public ParameterInfo[] ParametersAll { get; private set; }

        public void PrepareParameters()
        {
            ParametersAll = Method.GetParameters();
            ParametersNormal.AddRange(ParametersAll);

            if (OverrideType == OverrideType.RedirectToServer || OverrideType == OverrideType.RedirectHash)
            {
                ParameterHashOrServer = ParametersAll[0];
                if (ParameterHashOrServer.ParameterType != typeof(int))
                    throw new Exception("ModuleRedirectWithHash|ModuleRedirectToServer: type of first parameter must be 'int'");
                if (OverrideType == OverrideType.RedirectHash && false == ParameterHashOrServer.Name.Equals("hash"))
                    throw new Exception("ModuleRedirectWithHash: name of first parameter must be 'hash'");
                if (OverrideType == OverrideType.RedirectToServer && false == ParameterHashOrServer.Name.Equals("serverId"))
                    throw new Exception("ModuleRedirectToServer: name of first parameter must be 'serverId'");
                ParametersNormal.RemoveAt(0);
            }

            foreach (var p in ParametersNormal)
            {
                var handle = GenAction.CreateIf(p);
                if (ResultHandle != null && handle != null)
                    throw new Exception("Too Many Result Handle. " + Method.DeclaringType.Name + "::" + Method.Name);
                if (handle != null)
                    ResultHandle = handle;
            }
        }

        public string GetNormalCallString(Func<ParameterInfo, bool> skip = null)
        {
            StringBuilder sb = new StringBuilder();
            bool first = true;
            for (int i = 0; i < ParametersNormal.Count; ++i)
            {
                var p = ParametersNormal[i];
                if (null != skip && skip(p))
                    continue;
                if (first)
                    first = false;
                else
                    sb.Append(", ");
                string prefix = "";
                if (p.IsOut)
                    prefix = "out ";
                else if (p.ParameterType.IsByRef)
                    prefix = "ref ";

                if (string.IsNullOrEmpty(prefix))
                    sb.Append(p.Name);
                else
                    sb.Append(prefix).Append(p.Name);
            }
            return sb.ToString();
        }

        private string GetHashOrServerParameterName()
        {
            return OverrideType switch
            {
                OverrideType.RedirectHash => "hash",
                OverrideType.RedirectToServer => "serverId",
                _ => throw new Exception("error override type"),
            };
        }
        public string GetHashOrServerCallString()
        {
            if (ParameterHashOrServer == null)
                return "";
            if (ParametersAll.Length == 1) // 除了hash，没有其他参数。
                return GetHashOrServerParameterName();
            return $"{GetHashOrServerParameterName()}, ";
        }

        public string GetBaseCallString()
        {
            return $"{GetHashOrServerCallString()}{GetNormalCallString()}";
        }

        public string GetRedirectType()
        {
            return OverrideType switch
            {
                OverrideType.RedirectHash => "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeWithHash",
                OverrideType.RedirectToServer => "Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer",
                _ => throw new Exception("unkown OverrideType"),
            };
        }


        public string GetChoiceHashOrServerCodeSource()
        {
            return OverrideType switch
            {
                OverrideType.RedirectToServer => "serverId",
                OverrideType.RedirectHash => "hash",// parameter name
                _ => throw new Exception("error state"),
            };
        }

        public string GetConcurrentLevelSource()
        {
            if (OverrideType == OverrideType.RedirectToServer)
                throw new Exception("No GetConcurrentLevelSource");
            if (OverrideType == OverrideType.RedirectAll)
            {
                return (Attribute as RedirectAllAttribute).GetConcurrentLevelSource;
            }
            else
            {
                var source = (Attribute as RedirectHashAttribute).GetConcurrentLevelSource;
                if (string.IsNullOrEmpty(source))
                    return "1";
                return source;
            }
        }
    }
}
