
namespace Zeze.Gen.java
{
    public class HotDistribute : AbstractHotDistribute
    {
        private ClientService Client { get; } = new ClientService();
        private Net.Connector Connector { get; }

        public HotDistribute(string host, int port)
        {
            Client.Config.TryGetOrAddConnector(host, port, false, out var connector);
            Connector = connector;

            // 等待500ms，对于Gen的整体流程应该影响不大。
            Connector.TryGetReadySocket(500);
        }

        public bool HasDistribute()
        {
            return Connector.TryGetReadySocket() != null;
        }

        public void GenBean(Bean bean)
        {
            /*
			if (HasDistribute())
			{
				var lastVersion = GetLastVersionBean(bean.FullName);
				var curVersion = bean; // 当前xml的Bean。

				if (null == lastVersion)
				{
					if (curVersion.Name is versioned)
						throw new Exception(); // 第一个版本不能用版本方式命名。
					GenWithoutToPrevious();
					return; // done
				}
				var lastVars = lastVersion.Variables();
				var curVars = curVersion.Variables();
				var (add, remove) = diff(lastVars, curVars);
				var versionDistance = distanceVersion(lastVersion, curVersion); // 版本号差异。
				if (versionDistance > 1)
					throw new Exception("version distance > 1");
				if (add.empty() && remove.empty() && versionDistance > 0)
					throw new Exception("var no change, but bean version changed."); // 这个或者警告即可。

				if (versionDistance == 0)
					throw new Exception("var change, but bean version not change.");
				GenWithToPrevious(add, remove); // 这里包括add,remove都是空的，总是生成旧版兼容。

				return; // done
			}
			var curVersion = Gen.GetBean(); // 当前xml的Bean。
			if (curVersion.Name is versioned)
				throw new Exception(); // 开发期间，不能用版本方式命名。
			GenWithoutToPrevious();
			*/
        }

        private void GetLastVersionBean(string fullName)
        {

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
