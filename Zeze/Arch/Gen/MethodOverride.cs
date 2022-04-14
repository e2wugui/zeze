using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Gen
{
    public class MethodOverride
    {
        public MethodInfo Method { get; }
        public OverrideType OverrideType { get; }
        public Attribute Attribute { get; }
        public GenAction ResultHandle { get; private set; }

        public MethodOverride(MethodInfo method, OverrideType type, Attribute attribute)
        {
            if (false == method.IsVirtual)
                throw new Exception("ModuleRedirect Need Virtual。");
            Method = method;
            OverrideType = type;
            Attribute = attribute;
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

        public string GetNarmalCallString(Func<ParameterInfo, bool> skip = null)
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
            switch (OverrideType)
            {
                case OverrideType.RedirectHash: return "hash";
                case OverrideType.RedirectToServer: return "serverId";
                default: throw new Exception("error override type");
            }
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
            return $"{GetHashOrServerCallString()}{GetNarmalCallString()}";
        }

        public string GetRedirectType()
        {
            switch (OverrideType)
            {
                case OverrideType.RedirectHash:
                    return "Zezex.Provider.ModuleRedirect.RedirectTypeWithHash";

                case OverrideType.RedirectToServer:
                    return "Zezex.Provider.ModuleRedirect.RedirectTypeToServer";

                default:
                    throw new Exception("unkown OverrideType");
            }
        }


        public string GetChoiceHashOrServerCodeSource()
        {
            switch (OverrideType)
            {
                case OverrideType.RedirectToServer:
                    return "serverId";

                case OverrideType.RedirectHash:
                    return "hash"; // parameter name

                default:
                    throw new Exception("error state");
            }
        }

        public string GetConcurrentLevelSource()
        {
            if (OverrideType != OverrideType.RedirectAll)
                throw new Exception("is not RedirectAll");
            var attr = Attribute as RedirectAllHashAttribute;
            return attr.GetConcurrentLevelSource;
        }
    }
}
