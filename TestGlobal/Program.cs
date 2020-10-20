using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;

namespace TestGlobal
{
    class Program
    {
        class Stat : IComparable<Stat>
        {
            public string Name { get; set; }
            public int CountTrue { get; set; }
            public int CountFalse { get; set; }
            public int CountException { get; set; }

            public int CountTotal => CountTrue + CountFalse + CountException;

            public int CompareTo([AllowNull] Stat other)
            {
                return other.CountTotal.CompareTo(CountTotal);
            }

            public override string ToString()
            {
                return $"{Name} True={CountTrue} False={CountFalse} Exception={CountException}";
            }
        }

        private int funcInCSharp(IntPtr luaState)
        {
            KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
            long param = lua.ToInteger(-1);
            lua.PushInteger(++param);
            Console.WriteLine("funcInCSharp with " + param);
            return 1;
        }

        public void Testlua()
        {
            KeraLua.Lua lua = new KeraLua.Lua();

            lua.Register("funcInCSharp", funcInCSharp);

            lua.DoString("require 'funcInLua'");
            lua.GetGlobal("funcInLua");
            lua.PushInteger(1);
            lua.Call(1, 1);
            long result = lua.ToInteger(-1);
            Console.WriteLine("result of funcInLua: " + result);
            lua.Pop(1);

            //Console.WriteLine("result of dostring " + lua.DoString("funcInCSharp(2)"));

            bool dofile = lua.DoFile("funcInLuaMain.lua");
            Console.WriteLine("dofile " + dofile);
        }

        static void Main(string[] args)
        {
            new Program().Testlua();
            /*
            // Stat 
            System.IO.StreamReader sr = new System.IO.StreamReader("TransactionCnt.log", System.Text.Encoding.UTF8);
            string line;
            int lineNo = 0;
            Dictionary<string, Stat> stats = new Dictionary<string, Stat>();
            while ((line = sr.ReadLine()) != null)
            {
                if (lineNo++ == 0)
                    continue; // skip first line.

                string [] tok = line.Split(' ');
                if (tok.Length < 8)
                    continue;

                string pnameWithResult = tok[5];
                int pcount = int.Parse(tok[7]);
                string pname;
                int resulttype;

                if (pnameWithResult.EndsWith(".False"))
                {
                    pname = pnameWithResult.Substring(1, pnameWithResult.Length - 7);
                    resulttype = 1;
                }
                else if (pnameWithResult.EndsWith(".Exception"))
                {
                    pname = pnameWithResult.Substring(1, pnameWithResult.Length - 11);
                    resulttype = 2;
                }
                else
                {
                    pname = pnameWithResult.Substring(1);
                    resulttype = 0;
                }

                Stat stat;
                if (false == stats.TryGetValue(pname, out stat))
                {
                    stat = new Stat()
                    {
                        Name = pname,
                    };
                    stats.Add(pname, stat);
                }
                switch (resulttype)
                {
                    case 1: stat.CountFalse = pcount; break;
                    case 2: stat.CountException = pcount; break;
                    case 0: stat.CountTrue = pcount; break;
                }
            }
            Stat[] sort = stats.Values.ToArray();
            Array.Sort(sort);
            int totalTotal = 0;
            foreach (Stat stat in sort)
            {
                totalTotal += stat.CountTotal;
            }
            foreach (Stat stat in sort)
            {
                double percent = (double)stat.CountTotal / totalTotal;
                string p = string.Format("{0:0%}", percent);
                Console.WriteLine($"{p} {stat}");
            }
            

            // test global
            UnitTest.Zeze.Trans.TestGlobal g = new UnitTest.Zeze.Trans.TestGlobal();
            g.Test2App();
            */
        }
    }
}
