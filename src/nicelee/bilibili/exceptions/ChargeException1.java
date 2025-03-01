package nicelee.bilibili.exceptions;

/**
 * 充电视频无法下载异常
 */
public class ChargeException1 extends BilibiliError{
    private int qn=0;

    public int getQn() {
        return qn;
    }

    public ChargeException1(String message, int qn) {
        super(message);
        this.qn = qn;
    }

    public ChargeException1(String message, Throwable cause) {
        super(message, cause);
    }
}
