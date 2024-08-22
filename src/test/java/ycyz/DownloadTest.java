package ycyz;

import org.junit.Test;
import ycyz.client.DownloadArgs;
import ycyz.client.DownloadService;

public class DownloadTest {
    @Test
    public void testDownload(){
        DownloadService service = new DownloadService();
        service.login();
        service.downloazdByPage(DownloadArgs.builder().url("https://space.bilibili.com/2615982").build());
    }
}
