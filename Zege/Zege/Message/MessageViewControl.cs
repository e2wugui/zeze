using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zege.Message
{
    // 使用控件实现的消息窗口。
    public class MessageViewControl : IMessageView
    {
        private Layout Parent;
        private ScrollView ScrollView;
        private AbsoluteLayout MessageLayout;
        private double NextMessageY = 5.0;

        public MessageViewControl(Layout parent)
        {
            Parent = parent;
            ScrollView = new ScrollView()
            {
                HeightRequest = 225,
                WidthRequest = 600,
            };
            MessageLayout = new AbsoluteLayout();
            ScrollView.Content = MessageLayout;
        }

        private void MessageVisible(long messageId)
        {
            var locate = LocateMessage(messageId);
            if (locate >= 0)
            {
                var msg = Messages[locate];
                _ = ScrollView.ScrollToAsync(msg.Item3, ScrollToPosition.Start, true);
            }
        }

        public void Show(long messageId)
        {
            foreach (var children in Parent.Children)
            {
                // 第一个打开的消息窗口已经加入Parent了。
                // 此时处理可见消息即可。
                // 这个写法有些恶心。
                // 先这样吧。
                if (children == ScrollView)
                {
                    MessageVisible(messageId);
                    return;
                }
            }
            Parent.RemoveAt(0);
            Parent.Insert(0, ScrollView);
            MessageVisible(messageId);
        }

        public void InsertHead(bool self, long messageId, string message)
        {
            Insert(0, self, messageId, message);
        }

        public HashSet<IView> Selfs = new();
        public List<(long, Image, Editor)> Messages = new();

        public void AddTail(bool self, long messageId, string message)
        {
            Insert(Messages.Count, self, messageId, message);
        }

        private void Insert(int index, bool self, long messageId, string message)
        {
            var photo = new Image()
            {
                Source = "https://www.google.com/images/hpp/Chrome_Owned_96x96.png",
                HeightRequest = 30,
                WidthRequest = 30,
            };
            MessageLayout.Add(photo);
            var x = self ? ScrollView.Width - 40 : 0;
            MessageLayout.SetLayoutBounds(photo, new Rect(x, NextMessageY, 30, 30));
            var lastMessage = new Editor()
            {
                MinimumWidthRequest = 30,
                MaximumWidthRequest = 300,
                AutoSize = EditorAutoSizeOption.TextChanges,
                Text = message,
                BackgroundColor = self ? Colors.LightGreen : Colors.White,
            };
            lastMessage.SizeChanged += OnMessageSizeChanged;
            if (self)
                Selfs.Add(lastMessage);

            Messages.Insert(index, (messageId, photo, lastMessage));
        }

        public async void OnMessageSizeChanged(object sender, EventArgs e)
        {
            var lastMessage = (Editor)sender;
            var self = Selfs.Contains(lastMessage);
            var x = self ? ScrollView.Width - lastMessage.Width - 40 : 40;
            MessageLayout.SetLayoutBounds(lastMessage, new Rect(x, NextMessageY, lastMessage.Width, lastMessage.Height));
            NextMessageY += lastMessage.Height + 5;
            lastMessage.SizeChanged -= OnMessageSizeChanged;
            await ScrollView.ScrollToAsync(0, NextMessageY, true);
        }

        public void Remove(long messageId)
        {
            for (var i = 0; i < Messages.Count; ++i)
            {
                var msg = Messages[i];
                if (msg.Item1 == messageId)
                {
                    MessageLayout.Remove(msg.Item2); // image
                    MessageLayout.Remove(msg.Item3); // text
                    Messages.RemoveAt(i);
                }
            }
        }

        private int LocateMessage(long messageId)
        {
            for (var i = 0; i < Messages.Count; ++i)
            {
                if (Messages[i].Item1 == messageId)
                    return i;
            }
            return -1;
        }

        public void RemoveBefore(long messageId)
        {
            var locate = LocateMessage(messageId);
            if (locate > 0)
            {
                for (int i = locate - 1; i >= 0; --i)
                {
                    var msg = Messages[i];
                    MessageLayout.Remove(msg.Item2); // image
                    MessageLayout.Remove(msg.Item3); // text
                    Messages.RemoveAt(i);
                }
            }
        }
    }
}
