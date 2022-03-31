using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class MethodOverride
    {
        public MethodInfo Method { get; }
        public OverrideType OverrideType { get; }
        public Attribute Attribute { get; }

        public MethodOverride(MethodInfo method, OverrideType type, Attribute attribute)
        {
            if (false == method.IsVirtual)
                throw new Exception("ModuleRedirect Need Virtual。");
            Method = method;
            OverrideType = type;
            Attribute = attribute;
        }

        public ParameterInfo ParameterHashOrServer { get; private set; } // only setup when OverrideType.RedirectWithHash
        public List<ParameterInfo> ParametersNormal { get; } = new List<ParameterInfo>();
        public ParameterInfo ParameterLastWithMode { get; private set; } // maybe null

        public ParameterInfo[] ParametersAll { get; private set; }

        public void PrepareParameters()
        {
            ParametersAll = Method.GetParameters();
            ParametersNormal.AddRange(ParametersAll);

            if (OverrideType == OverrideType.RedirectToServer || OverrideType == OverrideType.RedirectWithHash)
            {
                ParameterHashOrServer = ParametersAll[0];
                if (ParameterHashOrServer.ParameterType != typeof(int))
                    throw new Exception("ModuleRedirectWithHash|ModuleRedirectToServer: type of first parameter must be 'int'");
                if (OverrideType == OverrideType.RedirectWithHash && false == ParameterHashOrServer.Name.Equals("hash"))
                    throw new Exception("ModuleRedirectWithHash: name of first parameter must be 'hash'");
                if (OverrideType == OverrideType.RedirectToServer && false == ParameterHashOrServer.Name.Equals("serverId"))
                    throw new Exception("ModuleRedirectToServer: name of first parameter must be 'serverId'");
                ParametersNormal.RemoveAt(0);
            }

            if (ParametersNormal.Count > 0
                && ParametersNormal[ParametersNormal.Count - 1].ParameterType == typeof(Zeze.TransactionModes))
            {
                ParameterLastWithMode = ParametersNormal[ParametersNormal.Count - 1];
                ParametersNormal.RemoveAt(ParametersNormal.Count - 1);
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

        public string GetModeCallString()
        {
            if (ParameterLastWithMode == null)
                return "";
            if (ParametersAll.Length == 1) // 除了mode，没有其他参数。
                return ParameterLastWithMode.Name;
            return $", {ParameterLastWithMode.Name}";
        }

        private string GetHashOrServerParameterName()
        {
            switch (OverrideType)
            {
                case OverrideType.RedirectWithHash: return "hash";
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
            return $"{GetHashOrServerCallString()}{GetNarmalCallString()}{GetModeCallString()}";
        }

        public string GetRedirectType()
        {
            switch (OverrideType)
            {
                case OverrideType.Redirect: // fall down
                case OverrideType.RedirectWithHash:
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

                case OverrideType.RedirectWithHash:
                    return "hash"; // parameter name

                case OverrideType.Redirect:
                    var attr = Attribute as RedirectAttribute;
                    if (string.IsNullOrEmpty(attr.ChoiceHashCodeSource))
                        return "Zezex.ModuleRedirect.GetChoiceHashCode()"; // Interface TODO
                    return attr.ChoiceHashCodeSource;

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
