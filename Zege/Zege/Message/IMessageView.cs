using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zege.Message
{
    // 消息窗口接口。
    // 现在先实现MessageViewControl（用控件实现的）。
    // 关于消息的更多显示方式，需要修改接口。
    // 现在仅是草稿。
    public interface IMessageView
    {
        // 翻阅历史消息时用来加入消息。
        public void InsertHead(BMessage message);

        // 收到最新的消息时，加入消息。
        public void AddTail(BMessage message);

        // 删除旧的消息，不缓存运行以来所有的消息，限制内存的使用。
        // 不包含此消息Id。
        public void RemoveBefore(long messageId);

        // 删除单个消息，特殊操作，暂未使用。
        public void Remove(long messageId);

        // 显示窗口，参数为"第一条"可见消息。
        // 如果这条可见消息以及后面的消息不够一页，但前面又有足够的消息时，第一条可见可以往前退一些。
        // 这个消息用于第一次（重新）打开消息窗口时，确定怎么显示消息。有如下两种模式：
        // 1. 参数为第一条未读消息，用于好友以及重要的群（部门）消息。
        // 2. 参数为最后一条消息，此时显示的是最近的消息（微信模式）。
        public void Show(long messageId);
    }
}
