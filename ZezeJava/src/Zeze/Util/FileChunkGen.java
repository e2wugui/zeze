package Zeze.Util;

import Zeze.*;
import java.util.*;
import java.io.*;

/** 
 一般用于生成代码时，需要解析存在的文件，根据Chunk名字替生成，同时保留其他行不变。
*/
public class FileChunkGen {
	public enum State {
		Normal(0),
		ChunkStart(1),
		ChunkEnd(2);

		public static final int SIZE = java.lang.Integer.SIZE;

		private int intValue;
		private static java.util.HashMap<Integer, State> mappings;
		private static java.util.HashMap<Integer, State> getMappings() {
			if (mappings == null) {
				synchronized (State.class) {
					if (mappings == null) {
						mappings = new java.util.HashMap<Integer, State>();
					}
				}
			}
			return mappings;
		}

		private State(int value) {
			intValue = value;
			getMappings().put(value, this);
		}

		public int getValue() {
			return intValue;
		}

		public static State forValue(int value) {
			return getMappings().get(value);
		}
	}
	public static class Chunk {
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private State State = State.values()[0];
		public final State getState() {
			return State;
		}
		public final void setState(State value) {
			State = value;
		}
		private String StartLine;
		public final String getStartLine() {
			return StartLine;
		}
		public final void setStartLine(String value) {
			StartLine = value;
		}
		private String EndLine;
		public final String getEndLine() {
			return EndLine;
		}
		public final void setEndLine(String value) {
			EndLine = value;
		}
		private ArrayList<String> Lines = new ArrayList<String> ();
		public final ArrayList<String> getLines() {
			return Lines;
		}
		public final void setLines(ArrayList<String> value) {
			Lines = value;
		}
	}
	private ArrayList<Chunk> Chunks = new ArrayList<Chunk> ();
	public final ArrayList<Chunk> getChunks() {
		return Chunks;
	}
	private String ChunkStartTag;
	public final String getChunkStartTag() {
		return ChunkStartTag;
	}
	private String ChunkEndTag;
	public final String getChunkEndTag() {
		return ChunkEndTag;
	}

	public FileChunkGen(String chunkStartTag) {
		this(chunkStartTag, "// ZEZE_FILE_CHUNK }}}");
	}

	public FileChunkGen() {
		this("// ZEZE_FILE_CHUNK {{{", "// ZEZE_FILE_CHUNK }}}");
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public FileChunkGen(string chunkStartTag = "// ZEZE_FILE_CHUNK {{{", string chunkEndTag = "// ZEZE_FILE_CHUNK }}}")
	public FileChunkGen(String chunkStartTag, String chunkEndTag) {
		if (chunkStartTag.length() == 0) {
			throw new IllegalArgumentException();
		}
		if (chunkEndTag.length() == 0) {
			throw new IllegalArgumentException();
		}

		this.ChunkStartTag = chunkStartTag;
		this.ChunkEndTag = chunkEndTag;
	}

	public final boolean LoadFile(String fileName) {
		return LoadFile(fileName, Encoding.UTF8);
	}

	@FunctionalInterface
	public interface ChunkProcess {
		void invoke(java.io.OutputStreamWriter writer, Chunk chunk);
	}

	public final void SaveFile(String fileName, ChunkProcess cp) {
		SaveFile(fileName, cp, Encoding.UTF8);
	}

	public final void SaveFile(String fileName, ChunkProcess cp, Encoding encoding) {
		try (OutputStreamWriter sw = new OutputStreamWriter(fileName)) {
			for (var chunk : getChunks()) {
				switch (chunk.getState()) {
					case Normal:
					case Normal:
					case Normal:
						for (var line : chunk.getLines()) {
							sw.write(line + System.lineSeparator());
						}
						break;
					case ChunkStart:
						throw new RuntimeException("chunk is not closed");
					case ChunkEnd:
						sw.write(chunk.getStartLine() + System.lineSeparator());
						cp.invoke(sw, chunk);
						sw.write(chunk.getEndLine() + System.lineSeparator());
						break;
				}
			}
		}
	}

	public final boolean LoadFile(String fileName, Encoding encoding) {
		if (!(new File(fileName)).isFile()) {
			return false;
		}

		getChunks().clear();

		try (InputStreamReader sr = new InputStreamReader(fileName)) {
			for (String line = sr.ReadLine(); !line.equals(null); line = sr.ReadLine()) {
				Zeze.Util.FileChunkGen.State lineState;
				tangible.OutObject<Zeze.Util.FileChunkGen.State> tempOut_lineState = new tangible.OutObject<Zeze.Util.FileChunkGen.State>();
				String lineName;
				tangible.OutObject<String> tempOut_lineName = new tangible.OutObject<String>();
				LineState(line, tempOut_lineState, tempOut_lineName);
			lineName = tempOut_lineName.outArgValue;
			lineState = tempOut_lineState.outArgValue;
				if (getChunks().isEmpty()) {
					switch (lineState) {
						case Normal:
						case Normal:
						case Normal:
							Chunk chunkNew = new Chunk();
							chunkNew.setName(lineName);
							chunkNew.setState(lineState);
							chunkNew.getLines().add(line);
							getChunks().add(chunkNew);
							break;
						case ChunkStart:
							Chunk tempVar = new Chunk();
							tempVar.setName(lineName);
							tempVar.setState(lineState);
							tempVar.setStartLine(line);
							getChunks().add(tempVar);
							break;
						case ChunkEnd:
							throw new RuntimeException("chunk not found but ChunkEnd");
					}
					continue;
				}
				Chunk current = getChunks().get(getChunks().size() - 1);
				switch (current.getState()) {
					case Normal:
					case Normal:
					case Normal:
						switch (lineState) {
							case Normal:
							case Normal:
							case Normal:
								current.getLines().add(line);
								break;
							case ChunkStart:
								Chunk tempVar2 = new Chunk();
								tempVar2.setName(lineName);
								tempVar2.setState(lineState);
								tempVar2.setStartLine(line);
								getChunks().add(tempVar2);
								break;
							case ChunkEnd:
								throw new RuntimeException("current chunk is Normal but ChunkEnd");
						}
						break;
					case ChunkStart:
						switch (lineState) {
							case Normal:
							case Normal:
							case Normal:
								current.getLines().add(line);
								break;
							case ChunkStart:
								throw new RuntimeException("current chunk is ChunkStart but ChunkStart");
							case ChunkEnd:
								current.setState(lineState);
								current.setEndLine(line);
								break;
						}
						break;
					case ChunkEnd:
						switch (lineState) {
							case Normal:
							case Normal:
							case Normal:
								Chunk chunkNew = new Chunk();
								chunkNew.setName(lineName);
								chunkNew.setState(lineState);
								chunkNew.getLines().add(line);
								getChunks().add(chunkNew);
								break;
							case ChunkStart:
								Chunk tempVar3 = new Chunk();
								tempVar3.setName(lineName);
								tempVar3.setState(lineState);
								tempVar3.setStartLine(line);
								getChunks().add(tempVar3);
								break;
							case ChunkEnd:
								throw new RuntimeException("current chunk is ChunkEnd but ChunkEnd");
						}
						break;
				}
			}
			if (!getChunks().isEmpty() && getChunks().get(getChunks().size() - 1).getState() == State.ChunkStart) {
				throw new RuntimeException("chunk is not closed");
			}
			return true;
		}
	}

	private void LineState(String line, tangible.OutObject<State> state, tangible.OutObject<String> name) {
		String lineTrim = line.strip();
		if (lineTrim.startsWith(getChunkStartTag())) {
			state.outArgValue = State.ChunkStart;
			name.outArgValue = lineTrim.substring(getChunkStartTag().length()).strip();
			return;
		}
		if (lineTrim.startsWith(getChunkEndTag())) {
			state.outArgValue = State.ChunkEnd;
			name.outArgValue = lineTrim.substring(getChunkEndTag().length()).strip();
			return;
		}
		state.outArgValue = State.Normal;
		name.outArgValue = "";
	}

}