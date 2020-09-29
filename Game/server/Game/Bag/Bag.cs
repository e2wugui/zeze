using System;
using System.Collections.Generic;
using System.Text;

namespace Game.Bag
{
    public class Bag
    {
        private BBag bag;

        public Bag(long roleid, tbag table)
        {
            bag = table.GetOrAdd(roleid);
        }

        public class Change
        { 

        }

        public void Move(int from, int to, int number)
        {

        }
    }
}
