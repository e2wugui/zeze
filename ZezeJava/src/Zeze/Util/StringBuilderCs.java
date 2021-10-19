package Zeze.Util;

public class StringBuilderCs {
    public StringBuilder sb = new StringBuilder();

    public void AppendLine(String line) {
        sb.append(line).append("\n");
    }

    public void Append(String s) {
        sb.append(s);
    }

    public void Append(int i) {
        sb.append(i);
    }

    public void Append(long l) {
        sb.append(l);
    }

    public String toString() {
        return sb.toString();
    }
}
