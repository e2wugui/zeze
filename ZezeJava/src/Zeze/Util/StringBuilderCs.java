package Zeze.Util;

public class StringBuilderCs {
    public StringBuilder sb = new StringBuilder();

    public StringBuilderCs AppendLine(String line) {
        sb.append(line).append("\n");
        return this;
    }

    public StringBuilderCs Append(String s) {
        sb.append(s);
        return this;
    }

    public StringBuilderCs Append(int i) {
        sb.append(i);
        return this;
    }

    public StringBuilderCs Append(long l) {
        sb.append(l);
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
