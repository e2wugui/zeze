using Microsoft.Maui.Graphics;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zege.Message
{
    public class MessageDrawable : IDrawable
    {
        public string Message { get; set; } = "调度超期(比如服务器down机重启)调度是如何进行的 -- simpleTimer cronTimer是否有不同";

        private const double Margin = 10;
        private const double LinePadding = 5;
        private bool Init = false;
        private Microsoft.Maui.Graphics.Font Font;
        private int FontSize;
        private SizeF CharSize;

        private bool IsLatin(char c)
        {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        private List<(float, float, string)> Lines = new();

        private void DrawLines(ICanvas canvas)
        {
            foreach (var line in Lines)
                canvas.DrawString(line.Item3, line.Item1, line.Item2, HorizontalAlignment.Left);
            Lines.Clear();
        }

        private void DrawLine(ref int lineStart, int lineEnd, double x, ref double y)
        {
            var line = Message.Substring(lineStart, lineEnd - lineStart);
            Lines.Add(((float)x, (float)y, line));
            if (lineEnd < Message.Length)
                y += CharSize.Height + LinePadding;
            lineStart = lineEnd;
        }

        private void DrawWordWrapLine(ref int lineStart, ref int lineEnd, double x, ref double y)
        {
            if (IsLatin(Message[lineEnd - 1]))
            {
                // 最后一个是单词。
                var nextCharIndex = lineEnd;
                if (nextCharIndex < Message.Length && false == IsLatin(Message[nextCharIndex]))
                {
                    // 下一个不是字母（单词没有被截取）。马上画出。
                    DrawLine(ref lineStart, lineEnd, x, ref y);
                    return;
                }

                // 往前找单词分割。
                var i = lineEnd - 1;
                for (; i >= lineStart && IsLatin(Message[i]); i--)
                {
                    // search...
                }
                if (i < lineStart)
                {
                    // 很长的单词，一行都放不下。强制画出。
                    DrawLine(ref lineStart, lineEnd, x, ref y);
                    return;
                }
                // 重新截取前面的单词分隔符，包括分隔符。
                DrawLine(ref lineStart, i + 1, x, ref y);
                lineStart = i + 1;
                lineEnd = i; // 外面循环结束马上会加1.这里指向上一个。
                return;
                // 继续后面的正常画出。
            }
            // 正常画出。
            DrawLine(ref lineStart, lineEnd, x, ref y);
        }

        public void Draw(ICanvas canvas, RectF dirtyRect)
        {
            if (false == Init)
            {
                Init = true;
                Font = Microsoft.Maui.Graphics.Font.Default;
                FontSize = 14;
                canvas.Font = Font;
                canvas.FontSize = FontSize;
                CharSize = canvas.GetStringSize("啊", Font, FontSize);
            }
            //canvas.Font = Font;
            //canvas.FontSize = FontSize;
            canvas.FontColor = Colors.Gray;
            canvas.DrawRectangle(dirtyRect);

            var rect = new Rect(dirtyRect.X, dirtyRect.Y, dirtyRect.Width * 0.6, dirtyRect.Height);
            var rectf = new Rect(rect.X + Margin, rect.Y + LinePadding, rect.Width - 2 * Margin, rect.Height);
            var x = rectf.X;
            var y = rectf.Y + CharSize.Height;
            var lineStart = 0;
            var lineEnd = 0;
            var maxLineWidth = 0.0;
            for (; lineEnd < Message.Length; ++lineEnd)
            {
                var c = Message[lineEnd];
                var line = Message.Substring(lineStart, lineEnd - lineStart + 1);
                var lineSize = canvas.GetStringSize(line, Font, FontSize);
                if (lineSize.Width > rectf.Width)
                {
                    // 超出了，画出行（不包括当前超出字符）。
                    DrawWordWrapLine(ref lineStart, ref lineEnd, x, ref y);
                    continue;
                }
                if (c == '\n')
                {
                    // 行结束，强制换行。lineEnd + 1：吃掉换行符。
                    DrawLine(ref lineStart, lineEnd + 1, x, ref y);
                    continue;
                }
                if (lineSize.Width > maxLineWidth)
                    maxLineWidth = lineSize.Width;
            }
            if (lineStart < lineEnd)
            {
                var line = Message.Substring(lineStart, lineEnd - lineStart);
                var lineSize = canvas.GetStringSize(line, Font, FontSize);
                DrawLine(ref lineStart, lineEnd, x, ref y);
                if (lineSize.Width > maxLineWidth)
                    maxLineWidth = lineSize.Width;
            }
            rect.Height = y - rect.Y + Margin + LinePadding;
            rect.Width = maxLineWidth + Margin * 2;
            canvas.FillColor = Colors.LightGreen;
            canvas.FillRectangle(rect);
            //canvas.DrawRoundedRectangle((float)rect.X, (float)rect.Y, (float)rect.Width, (float)rect.Height, 3f);
            canvas.FontColor = Colors.Black;
            DrawLines(canvas);

            // 1. 消息框宽度 60%
            // 2. 消息框 Align
            // 3. 消息框高度 Word Wrap？
            // 4. 消息框背景色
            // 5. 可见消息框数量
            // 6. 滚动条
            // 7. 往前浏览，往后浏览
        }
    }
}
