﻿using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace Zeze.Gen
{
    public class Protocol
    {
        public ModuleSpace Space { get; private set; }
        public string Name { get; private set; }

        public Protocol(ModuleSpace space, XmlElement self)
        {
            Space = space;
            Name = self.GetAttribute("name").Trim();
            space.Add(this);

        }
    }
}
