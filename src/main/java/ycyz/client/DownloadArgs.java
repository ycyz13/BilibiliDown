package ycyz.client;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DownloadArgs {
    /**
     * 投稿开始时间
     */
    public LocalDateTime startTime;

    /**
     * 编号
     */
    private String bizNo;

    /**
     * up主用户id
     */
    private String uid;

    /**
     * up名称
     */
    private String upName;
}
