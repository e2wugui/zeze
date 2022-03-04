using System.IO;

namespace Zeze.Gen.rrcs
{
    public class Maker
    {
        public Project Project { get; }

        public Maker(Project project)
        {
            Project = project;
        }

        public void Make()
        {
            string projectBasedir = Project.Gendir;
            string projectDir = Path.Combine(projectBasedir, Project.Name);
            string genDir = Path.Combine(projectDir, "Gen");
            string srcDir = projectDir;

            Program.AddGenDir(genDir);

            foreach (Types.Bean bean in Project.AllBeans.Values)
                new BeanFormatter(bean).Make(genDir);

            foreach (Protocol protocol in Project.AllProtocols.Values)
            {
                if (protocol is Rpc rpc)
                    new cs.RpcFormatter(rpc).Make(genDir);
                else
                    new cs.ProtocolFormatter(protocol).Make(genDir);
            }

            new App(Project, genDir, srcDir).Make();
        }
    }
}
