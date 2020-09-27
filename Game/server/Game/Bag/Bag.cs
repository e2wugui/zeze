using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Bag
{
    public class Bag
    {
        private BBag data;

        public Bag(long roleid, tbag table)
        {
            data = table.GetOrAdd(roleid);
        }
    }
}
