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
            Console.WriteLine("TODO cs make");
        }
    }
}
