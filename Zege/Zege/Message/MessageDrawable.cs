using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zege.Message
{
    public class MessageDrawable : IDrawable
    {
        public string Message { get; set; } = "啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一";

        private const double Margin = 5;
        private bool Init = false;
        private Microsoft.Maui.Graphics.Font Font;
        private int FontSize;
        private SizeF CharSize;

        private bool IsLatin(char c)
        {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        private void DrawLine(ICanvas canvas, string line, double x, ref double y)
        {
            canvas.DrawString(line, (float)x, (float)y, HorizontalAlignment.Left);
            if (line.Length < Message.Length)
            {
                y += CharSize.Height + Margin;
                Message = Message.Substring(line.Length);
                return;
            }
            Message = string.Empty;
        }

        private void DrawWordWrapLine(ICanvas canvas, string line, double x, ref double y)
        {
            if (line.Length > 0 && IsLatin(line[line.Length - 1]))
            {
                // 最后一个是单词。
                var nextCharIndex = line.Length;
                if (nextCharIndex < Message.Length && false == IsLatin(Message[nextCharIndex]))
                {
                    // 下一个不是字母。可以马上画出。
                    DrawLine(canvas, line, x, ref y);
                    return;
                }

                // 往前找单词分割。
                var i = line.Length - 1;
                for (; i >= 0 && IsLatin(line[i]); i--)
                {
                    // search...
                }
                if (i < 0)
                {
                    // 很长的单词，一行都放不下。强制画出。
                    DrawLine(canvas, line, x, ref y);
                    return;
                }
                line = line.Substring(0, i + 1);
                // 继续后面的正常画出。
            }
            // 正常画出。
            DrawLine(canvas, line, x, ref y);
        }

        public void Draw(ICanvas canvas, RectF dirtyRect)
        {
            if (false == Init)
            {
                Init = true;
                Font = Microsoft.Maui.Graphics.Font.Default;
                FontSize = 16;

                canvas.Font = Font;
                canvas.FontSize = FontSize;

                CharSize = canvas.GetStringSize("啊", Font, FontSize);
            }
            canvas.FontColor = Colors.Blue;
            canvas.DrawRectangle(dirtyRect);

            var rect = new Rect(dirtyRect.X, dirtyRect.Y, dirtyRect.Width / 2, dirtyRect.Height);
            var rectf = new Rect(rect.X + Margin, rect.Y + Margin, rect.Width - 2 * Margin, rect.Height);
            var charsLine = (int)(rectf.Width / CharSize.Width);
            var x = rectf.X;
            var y = rectf.Y + CharSize.Height;
            while (false == string.IsNullOrEmpty(Message))
            {
                var lenLine = charsLine;
                var line = lenLine < Message.Length ? Message.Substring(0, lenLine) : Message;
                var lineSize = canvas.GetStringSize(line, Font, FontSize);
                if (lineSize.Width < rectf.Width)
                {
                    string lastLine;
                    while (true)
                    {
                        // 记住最后行，往后找，直到画不下，然后画出最后一行。
                        lastLine = line;
                        if (lenLine < Message.Length)
                        {
                            ++lenLine;
                            line = lenLine < Message.Length ? Message.Substring(0, lenLine) : Message;
                            lineSize = canvas.GetStringSize(line, Font, FontSize);
                            if (lineSize.Width > rectf.Width)
                            {
                                DrawWordWrapLine(canvas, lastLine, x, ref y);
                                break;
                            }
                        }
                        else
                        {
                            // 消息已经不够长了。
                            DrawLine(canvas, lastLine, x, ref y);
                            break;
                        }
                    }
                }
                else
                {
                    while (true)
                    {
                        // 往前找，直到能画得下，然后画出当前行。
                        --lenLine;
                        line = lenLine < Message.Length ? Message.Substring(0, lenLine) : Message;
                        lineSize = canvas.GetStringSize(line, Font, FontSize);
                        if (lineSize.Width < rectf.Width)
                        {
                            DrawWordWrapLine(canvas, line, x, ref y);
                            break;
                        }
                    }
                }
            }
            rect.Height = y - rect.Y + Margin * 2;
            canvas.DrawRectangle(rect);

            // 1. 消息框宽度 80%
            // 2. 消息框 Align
            // 3. 消息框高度 Word Wrap？
            // 4. 消息框背景色
            // 5. 可见消息框数量
            // 6. 滚动条
            // 7. 往前浏览，往后浏览
        }
    }
}
