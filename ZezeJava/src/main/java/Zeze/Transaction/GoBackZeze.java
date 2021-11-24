package Zeze.Transaction;

public class GoBackZeze extends Error {

    public GoBackZeze(Throwable cause) {
        super(cause);
    }

    public GoBackZeze(String msg) {
        super(msg);
    }

    public GoBackZeze(String msg, Throwable cause) {
        super(msg, cause);
    }

    public GoBackZeze() {

    }

    public static void Throw(String msg, Throwable cause) {
        if (msg != null) {
            if (null != cause)
                throw new GoBackZeze(msg, cause);
            throw new GoBackZeze(msg);
        } else {
            if (null != cause)
                throw new GoBackZeze(cause);
            throw new GoBackZeze();
        }
    }
}
