﻿using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Text;

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
            Console.WriteLine("funcInCSharp with table 1 " + lua.IsTable(-1));
            Console.WriteLine("funcInCSharp with table len 1 " + lua.Length(-1));
            lua.GetField(-1, ""); // meta
            Console.WriteLine("funcInCSharp with table 2 " + lua.IsTable(-1));
            Console.WriteLine("funcInCSharp with table len 2 " + lua.Length(-1));
            lua.PushInteger(1);
            lua.GetTable(-2); // var type
            Console.WriteLine("funcInCSharp with table 3 " + lua.IsTable(-1));
            Console.WriteLine("funcInCSharp with table len 3 " + lua.Length(-1));
            lua.PushInteger(1);
            lua.GetTable(-2); // type
            Console.WriteLine("funcInCSharp with IsInteger 4 " + lua.IsInteger(-1));
            //Console.WriteLine("funcInCSharp with integer len 4 " + lua.Length(-1));
            Console.WriteLine("type = " + lua.ToInteger(-1));
            lua.Pop(3);
            lua.PushInteger(123);
            //throw new Exception("exception test");
            return 1;
        }

        public void Testlua()
        {
            KeraLua.Lua lua = new KeraLua.Lua();

            lua.Register("funcInCSharp", funcInCSharp);

            lua.DoString("require 'funcInLua'");
            lua.GetGlobal("funcInLua1");
            Console.WriteLine("is funcInLua1 " + lua.IsFunction(-1));
            lua.PushInteger(456);
            lua.Call(1, 1);
            long result = lua.ToInteger(-1);
            Console.WriteLine("result of funcInLua: " + result);
            lua.Pop(1);

            try
            {
                bool dofile = lua.DoFile("funcInLuaMain.lua");
                Console.WriteLine("dofile " + dofile);
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        static void Main(string[] args)
        {
            Console.WriteLine("enter main loop.");
            new UnitTest.Zeze.Lua.Main().MainLoop();
            /*
            new Program().Testlua();
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
