using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class ProviderModuleState
    {
        public long SessionId { get; }
        public int ModuleId { get; }
        public int ChoiceType { get; }
        public int ConfigType { get; }

        public ProviderModuleState(long sessionId, int moduleId, int choiceType, int configType)
        {
            SessionId = sessionId;
            ModuleId = moduleId;
            ChoiceType = choiceType;
            ConfigType = configType;
        }
    }
}
