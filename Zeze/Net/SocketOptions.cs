using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Net
{
    public class SocketOptions
    {
        // 系统选项
        public bool? NoDelay { get; set; }
        public int? SendBuffer { get; set; }
        public int? ReceiveBuffer { get; set; }

        // 应用选项
        public int InputBufferInitCapacity { get; set; } // 输入buffer初始化大小和自增长大小
        public int InputBufferResetThreshold { get; set; } // 输入buffer的容量超过这个数值后，如果可能重置buffer。
        public int InputBufferMaxCapacity { get; set; }
        public int OutputBufferMaxSize { get; set; } // 暂时不适用，先保留这个选项

        // 系统选项，但没有默认，只有 ServerSocket 使用。
        public int Backlog { get; set; }

        public SocketOptions()
        {
            // 这几个是应用层的选项，提供默认值。
            // 其他系统的选项不指定的话由系统提供默认值。
            InputBufferInitCapacity = 1024; // 对于绝大多数请求协议都够用了。对于大流量应用，应该加大。
            InputBufferResetThreshold = 16 * InputBufferInitCapacity;
            InputBufferMaxCapacity = 2 * 1024 * 1024; // 2M
            Backlog = 128;
        }
    }
}
