package nicelee.bilibili.exceptions;

/**
 * 充电视频无法下载异常
 */
public class ChargeException extends BilibiliError{

    public ChargeException(String message) {
        super(message);
    }

    public ChargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
