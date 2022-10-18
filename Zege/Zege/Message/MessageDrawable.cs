using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Drawing;
using Microsoft.Maui.Graphics;

namespace Zege.Message
{
    public class MessageDrawable : IDrawable
    {
        public string Message { get; set; } = "啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一啊一";

        private bool IsLatin(char c)
        {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
        }

        public void Draw(ICanvas canvas, RectF dirtyRect)
        {
            var font = Microsoft.Maui.Graphics.Font.Default;
            var fontSize = 18;
            canvas.Font = font;
            canvas.FontSize = fontSize;
            canvas.FontColor = Colors.Blue;
            canvas.DrawRectangle(dirtyRect);

            var charSize = canvas.GetStringSize("啊", font, fontSize);
            var rectf = new Rect(dirtyRect.X, dirtyRect.Y, dirtyRect.Height, dirtyRect.Width / 2);
            var charsLine = (int)(rectf.Width / charSize.Width);
            var x = rectf.X;
            var y = rectf.Y + charSize.Height;
            var done = false;
            while (!done)
            {
                var lenLine = charsLine;
                var line = lenLine < Message.Length ? Message.Substring(0, lenLine) : Message;
                var lineSize = canvas.GetStringSize(line, font, fontSize);
                if (lineSize.Width < rectf.Width)
                {
                    string lastLine;
                    int lastLenLine;
                    while (true)
                    {
                        // 记住最后行，往后找，直到画不下，然后画出最后一行。
                        lastLine = line;
                        lastLenLine = lenLine;
                        if (lenLine < Message.Length)
                        {
                            ++lenLine;
                            line = lenLine < Message.Length ? Message.Substring(0, lenLine) : Message;
                            lineSize = canvas.GetStringSize(line, font, fontSize);
                            if (lineSize.Width > rectf.Width)
                            {
                                if (lastLine.Length > 0 && IsLatin(lastLine[lastLine.Length - 1]))
                                {
                                    // 最后一个字母是单词。
                                    var nextCharIndex = lenLine;
                                    if (nextCharIndex < lastLine.Length && false == IsLatin(lastLine[nextCharIndex]))
                                    {
                                        // 下一个不是字母。可以马上画出。
                                        canvas.DrawString(lastLine, (float)x, (float)y, HorizontalAlignment.Left);
                                        if (lastLenLine < Message.Length)
                                        {
                                            Message = Message.Substring(lastLenLine);
                                            y += charSize.Height;
                                            break;
                                        }
                                        done = true;
                                        break; // all done
                                    }
                                    else
                                    {
                                        // 往前找单词分割。
                                        var i = lastLine.Length - 1;
                                        for (; i >= 0 && IsLatin(lastLine[i]); i--)
                                        { 
                                        }
                                        if (i < 0)
                                        {
                                            // 很长的单词，一行都放不下。强制画出。
                                            canvas.DrawString(lastLine, (float)x, (float)y, HorizontalAlignment.Left);
                                            if (lastLenLine < Message.Length)
                                            {
                                                Message = Message.Substring(lastLenLine);
                                                y += charSize.Height;
                                                break;
                                            }
                                            done = true;
                                            break; // all done
                                        }
                                        lastLenLine = i + 1;
                                        lastLine = lastLine.Substring(0, lastLenLine);
                                        // 继续后面的正常画出。
                                    }
                                }
                                // 正常画出。
                                canvas.DrawString(lastLine, (float)x, (float)y, HorizontalAlignment.Left);
                                if (lastLenLine < Message.Length)
                                {
                                    Message = Message.Substring(lastLenLine);
                                    y += charSize.Height;
                                    break;
                                }
                                done = true;
                                break; // all done
                            }
                        }
                        else
                        {
                            // 消息已经不够了。
                            canvas.DrawString(lastLine, (float)x, (float)y, HorizontalAlignment.Left);
                            if (lastLenLine < Message.Length)
                            {
                                Message = Message.Substring(lastLenLine);
                                y += charSize.Height;
                                break;
                            }
                            done = true;
                            break; // all done
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
                        lineSize = canvas.GetStringSize(line, font, fontSize);
                        if (lineSize.Width < rectf.Width)
                        {
                            if (line.Length > 0 && IsLatin(line[line.Length - 1]))
                            {
                                // 最后一个字母是单词。
                                var nextCharIndex = lenLine + 1;
                                if (nextCharIndex < line.Length && false == IsLatin(line[nextCharIndex]))
                                {
                                    // 下一个不是字母。可以马上画出。
                                    canvas.DrawString(line, (float)x, (float)y, HorizontalAlignment.Left);
                                    if (lenLine < Message.Length)
                                    {
                                        Message = Message.Substring(lenLine);
                                        y += charSize.Height;
                                        break;
                                    }
                                    done = true;
                                    break; // all done
                                }
                                else
                                {
                                    // 往前找单词分割。
                                    var i = line.Length - 1;
                                    for (; i >= 0 && IsLatin(line[i]); i--)
                                    {
                                    }
                                    if (i < 0)
                                    {
                                        // 很长的单词，一行都放不下。强制画出。
                                        canvas.DrawString(line, (float)x, (float)y, HorizontalAlignment.Left);
                                        if (lenLine < Message.Length)
                                        {
                                            Message = Message.Substring(lenLine);
                                            y += charSize.Height;
                                            break;
                                        }
                                        done = true;
                                        break; // all done
                                    }
                                    lenLine = i + 1;
                                    line = line.Substring(0, lenLine);
                                    // 继续后面的正常画出。
                                }
                            }
                            canvas.DrawString(line, (float)x, (float)y, HorizontalAlignment.Left);
                            if (lenLine < Message.Length)
                            {
                                Message = Message.Substring(lenLine);
                                y += charSize.Height;
                                break; // 外层while将会继续。
                            }
                            done = true;
                            break; // all done
                        }
                    }
                }
            }
            rectf.Height = y - rectf.Y;
            canvas.DrawRectangle(rectf);

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
