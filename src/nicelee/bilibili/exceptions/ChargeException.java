package nicelee.bilibili.exceptions;

public class ChargeException extends BilibiliError{

    public ChargeException(String message) {
        super(message);
    }

    public ChargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
