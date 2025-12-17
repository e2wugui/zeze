
using System;
using System.Collections.Generic;
using System.IO;
using Zeze.Builtin.HotDistribute;
using Zeze.Gen.Types;
using Zeze.Util;

namespace Zeze.Gen.java
{
    public class HotDistribute : AbstractHotDistribute
    {
        private ClientService Client { get; } = new ClientService();
        private Net.Connector Connector { get; }
        public Project Project { get; }
        public string BaseDir { get; }

        public HotDistribute(string host, int port, Project project, string baseDir)
        {
            Client.Config.TryGetOrAddConnector(host, port, false, out var connector);
            Connector = connector;

            // 等待500ms，对于Gen的整体流程应该影响不大。
            // 对方积极拒绝，时间应该更短。没有验证过。
            Connector.TryGetReadySocket(500);

            Project = project;
            BaseDir = baseDir;
        }

        public bool HasDistribute()
        {
            return Connector.TryGetReadySocket(0) != null;
        }

        // lastVars, curVars 都是按var.id排序的。
        public static (List<int>, List<int>) diff(IList<BVariable> lastVars, IList<Variable> curVars)
        {
            var add = new List<int>();
            var remove = new List<int>();

            var lastIt = lastVars.GetEnumerator();
            var curIt = curVars.GetEnumerator();

            var lastHas = lastIt.MoveNext();
            var curHas = curIt.MoveNext();
            while (lastHas && curHas)
            {
                if (lastIt.Current.Id == curIt.Current.Id)
                {
                    lastHas = lastIt.MoveNext();
                    curHas = curIt.MoveNext();
                    continue;
                }

                if (lastIt.Current.Id < curIt.Current.Id)
                {
                    remove.Add(lastIt.Current.Id);
                    lastHas = lastIt.MoveNext();
                }
                else
                {
                    add.Add(curIt.Current.Id);
                    curHas = curIt.MoveNext();
                }
            }
            while (lastHas)
            {
                remove.Add(lastIt.Current.Id);
                lastHas = lastIt.MoveNext();
            }
            while (curHas)
            {
                add.Add(curIt.Current.Id);
                curHas = curIt.MoveNext();
            }
            return (add, remove);
        }

        public class ToPrevious
        {
            public BLastVersionBeanInfo LastVersion { get; set; }
            public List<int> Add { get; set; }
            public List<int> Remove { get; set; }
        }

        public void GenBean(Bean bean)
        {
			if (HasDistribute())
			{
				var lastVersion = GetLastVersionBean(bean.FullName); // 注意：这个类型不是Bean。
				var curVersion = bean; // 当前xml的Bean。

				if (null == lastVersion)
				{
                    new BeanFormatter(bean).Make(BaseDir, Project);
					return; // done
				}
                // 检测是否出现无法热更(spring-loaded)的修改。
                /*
				var lastVars = lastVersion.Variables;
				var curVars = curVersion.VariablesIdOrder;
				var (add, remove) = diff(lastVars, curVars);
                */
                new BeanFormatter(bean).Make(BaseDir, Project);
			}
            else
            {
                new BeanFormatter(bean).Make(BaseDir, Project); // GenWithoutToPrevious();
            }
        }

        private BLastVersionBeanInfo GetLastVersionBean(string fullName)
        {
            var r = new GetLastVersionBeanInfo();
            r.Argument.Name = fullName;
            r.SendAsync(Connector.TryGetReadySocket(0)).Wait();

            switch (r.ResultCode)
            {
                case ResultCode.LogicError:
                    return null;
                case ResultCode.Success:
                    return r.Result;
                default:
                    throw new System.Exception($"GetLastVersionBean {r.ResultCode}");
            }
        }

        public class ClientService : Zeze.Net.Service
        {
            public ClientService()
                : base("Zeze.Gen.java.Distribute.ClientService", (Config)null)
            {
            }
        }
    }
}
