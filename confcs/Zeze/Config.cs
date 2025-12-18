using System;
using System.Collections.Concurrent;
using System.IO;
using System.Xml;
using Zeze.Net;
using Zeze.Util;

namespace Zeze
{
    public sealed class Config
    {
        public interface ICustomize
        {
            string Name { get; }
            void Parse(XmlElement self);
        }

        public ConcurrentDictionary<string, XmlElement> Customizes { get; } = new ConcurrentDictionary<string, XmlElement>();
        public LogLevel ProcessReturnErrorLogLevel { get; set; } = LogLevel.Info;

        /// <summary>
        /// 根据自定义配置名字查找。
        /// 因为外面需要通过AddCustomize注册进来，
        /// 如果外面保存了配置引用，是不需要访问这个接口的。
        /// </summary>
        /// <param name="customize"></param>
        public void ParseCustomize(ICustomize customize)
        {
            if (Customizes.TryGetValue(customize.Name, out var xmlElement))
                customize.Parse(xmlElement);
        }

        public ConcurrentDictionary<string, ServiceConf> ServiceConfMap { get; }
            = new ConcurrentDictionary<string, ServiceConf>();
        public ServiceConf DefaultServiceConf { get; internal set; } = new ServiceConf();

        public ServiceConf GetServiceConf(string name)
        {
            return ServiceConfMap.TryGetValue(name, out var serviceConf) ? serviceConf : null;
        }

        /// <summary>
        /// 由于这个方法没法加入Customize配置，为了兼容和内部测试保留，
        /// 应用应该自己LoadAndParse。
        /// var c = new Config();
        /// c.AddCustomize(...);
        /// c.LoadAndParse();
        /// </summary>
        /// <param name="xmlFile"></param>
        /// <returns></returns>
        public static Config Load(string xmlFile = "zeze.xml")
        {
            return new Config().LoadAndParse(xmlFile);
        }

        public Config LoadAndParse(string xmlFile = "zeze.xml")
        {
            if (File.Exists(xmlFile))
            {
                var doc = new XmlDocument();
                doc.Load(xmlFile);
                Parse(doc.DocumentElement);
            }
            return this;
        }

        public void Parse(XmlElement self)
        {
            if (!self.Name.Equals("zeze"))
                throw new Exception("is it a zeze config.");

            var attr = self.GetAttribute("ProcessReturnErrorLogLevel");
            if (attr.Length > 0)
                ProcessReturnErrorLogLevel = (LogLevel)Enum.Parse(typeof(LogLevel), attr);

            foreach (XmlNode node in self.ChildNodes)
            {
                if (XmlNodeType.Element != node.NodeType)
                    continue;

                var e = (XmlElement)node;
                switch (e.Name)
                {
                    case "ServiceConf":
                        _ = new ServiceConf(this, e);
                        break;

                    case "CustomizeConf":
                        var cname = e.GetAttribute("Name").Trim();
                        if (!Customizes.TryAdd(cname, e))
                            throw new Exception($"Unknown CustomizeConf Name='{cname}'");
                        break;

                    default:
                        throw new Exception("unknown node name: " + e.Name);
                }
            }

        }
    }
}
