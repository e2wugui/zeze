using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public sealed class Random
    {
        public static System.Random Instance { get; } = new System.Random();
    }
}
