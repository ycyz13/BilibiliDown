package ycyz.client;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class DownloadArgs {
    /**
     * 投稿开始时间
     */
    public Date startTime;

    public String url;
}
