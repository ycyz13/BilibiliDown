package nicelee.bilibili.exceptions;

/**
 * 充电视频无法下载异常
 */
public class VedioDeleteException extends BilibiliError{

    public VedioDeleteException(String message) {
        super(message);
    }

    public VedioDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
