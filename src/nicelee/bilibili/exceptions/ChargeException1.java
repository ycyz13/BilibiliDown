package nicelee.bilibili.exceptions;

/**
 * 充电视频无法下载异常
 */
public class ChargeException1 extends BilibiliError{

    public ChargeException1(String message) {
        super(message);
    }

    public ChargeException1(String message, Throwable cause) {
        super(message, cause);
    }
}
