using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    /// <summary>
    /// 一般用于生成代码时，需要解析存在的文件，根据Chunk名字替生成，同时保留其他行不变。
    /// </summary>
    public class FileChunkGen
    {
        public enum State
        {
            Normal = 0,
            ChunkStart,
            ChunkEnd,
        }
        public class Chunk
        {
            public string Name { get; set; }
            public State State { get; set; }
            public string StartLine { get; set; }
            public string EndLine { get; set; }
            public List<string> Lines { get; set; } = new List<string>();
        }
        public List<Chunk> Chunks { get; } = new List<Chunk>();
        public string ChunkStartTag { get; }
        public string ChunkEndTag { get; }
        public FileChunkGen(string chunkStartTag = "// ZEZE_FILE_CHUNK {{{", string chunkEndTag = "// ZEZE_FILE_CHUNK }}}")
        {
            if (chunkStartTag.Length == 0)
                throw new ArgumentException();
            if (chunkEndTag.Length == 0)
                throw new ArgumentException();

            this.ChunkStartTag = chunkStartTag;
            this.ChunkEndTag = chunkEndTag;
        }

        public bool LoadFile(string fileName)
        {
            return LoadFile(fileName, Encoding.UTF8);
        }

        public delegate void ChunkProcess(System.IO.StreamWriter writer, Chunk chunk);

        public void SaveFile(string fileName, ChunkProcess chunkProcess,
            ChunkProcess before = null, ChunkProcess after = null)
        {
            using System.IO.StreamWriter sw = Gen.Program.OpenStreamWriter(fileName, true);
            foreach (var chunk in Chunks)
            {
                switch (chunk.State)
                {
                    case State.Normal:
                        foreach (var line in chunk.Lines)
                            sw.WriteLine(line);
                        break;
                    case State.ChunkStart:
                        throw new Exception("chunk is not closed");
                    case State.ChunkEnd:
                        before?.Invoke(sw, chunk);
                        sw.WriteLine(chunk.StartLine);
                        chunkProcess(sw, chunk);
                        sw.WriteLine(chunk.EndLine);
                        after?.Invoke(sw, chunk);
                        break;
                }
            }
        }

        public bool LoadFile(string fileName, Encoding encoding)
        {
            if (!System.IO.File.Exists(fileName))
                return false;

            Chunks.Clear();

            using System.IO.StreamReader sr = new System.IO.StreamReader(fileName);
            for (string line = sr.ReadLine(); null != line; line = sr.ReadLine())
            {
                LineState(line, out var lineState, out var lineName);
                if (Chunks.Count == 0)
                {
                    switch (lineState)
                    {
                        case State.Normal:
                            Chunk chunkNew = new Chunk() { Name = lineName, State = lineState };
                            chunkNew.Lines.Add(line);
                            Chunks.Add(chunkNew);
                            break;
                        case State.ChunkStart:
                            Chunks.Add(new Chunk() { Name = lineName, State = lineState, StartLine = line });
                            break;
                        case State.ChunkEnd:
                            throw new Exception("chunk not found but ChunkEnd: " + fileName);
                    }
                    continue;
                }
                Chunk current = Chunks[Chunks.Count - 1];
                switch (current.State)
                {
                    case State.Normal:
                        switch (lineState)
                        {
                            case State.Normal:
                                current.Lines.Add(line);
                                break;
                            case State.ChunkStart:
                                Chunks.Add(new Chunk() { Name = lineName, State = lineState, StartLine = line });
                                break;
                            case State.ChunkEnd:
                                throw new Exception("current chunk is Normal but ChunkEnd: " + fileName);
                        }
                        break;
                    case State.ChunkStart:
                        switch (lineState)
                        {
                            case State.Normal:
                                current.Lines.Add(line);
                                break;
                            case State.ChunkStart:
                                throw new Exception("current chunk is ChunkStart but ChunkStart: " + fileName);
                            case State.ChunkEnd:
                                current.State = lineState;
                                current.EndLine = line;
                                break;
                        }
                        break;
                    case State.ChunkEnd:
                        switch (lineState)
                        {
                            case State.Normal:
                                Chunk chunkNew = new Chunk() { Name = lineName, State = lineState };
                                chunkNew.Lines.Add(line);
                                Chunks.Add(chunkNew);
                                break;
                            case State.ChunkStart:
                                Chunks.Add(new Chunk() { Name = lineName, State = lineState, StartLine = line });
                                break;
                            case State.ChunkEnd:
                                throw new Exception("current chunk is ChunkEnd but ChunkEnd: " + fileName);
                        }
                        break;
                }
            }
            if (Chunks.Count > 0 && Chunks[Chunks.Count - 1].State == State.ChunkStart)
                throw new Exception("chunk is not closed: " + fileName);
            return true;
        }

        private void LineState(string line, out State state, out string name)
        {
            string lineTrim = line.Trim();
            if (lineTrim.StartsWith(ChunkStartTag))
            {
                state = State.ChunkStart;
                name = lineTrim.Substring(ChunkStartTag.Length);
                int p = name.IndexOf("@formatter:");
                if (p >= 0)
                    name = name.Substring(0, p);
                name = name.Trim();
                return;
            }
            if (lineTrim.StartsWith(ChunkEndTag))
            {
                state = State.ChunkEnd;
                name = lineTrim.Substring(ChunkEndTag.Length);
                int p = name.IndexOf("@formatter:");
                if (p >= 0)
                    name = name.Substring(0, p);
                name = name.Trim();
                return;
            }
            state = State.Normal;
            name = "";
        }
    }
}
