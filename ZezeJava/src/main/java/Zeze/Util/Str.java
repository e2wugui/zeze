package Zeze.Util;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;

public class Str {
    public final static ParameterizedMessageFactory Formatter = new ParameterizedMessageFactory();

    public static String format(String f, Object ... params) {
        return Formatter.newMessage(f, params).getFormattedMessage();
    }
}
