using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Gen.cs
{
    public class Maker
    {
        public Project Project { get;  }

        public Maker(Project project)
        {
            Project = project;
        }

        public void make()
        {
            string projectDir = Project.Name;
            string genDir = System.IO.Path.Combine(projectDir, "Gen");
            if (System.IO.Directory.Exists(genDir))
                System.IO.Directory.Delete(genDir, true);

            foreach (Types.Bean bean in Project.AllBeans)
            {
                new BeanFormatter(bean).make(genDir);
            }
        }
    }
}
