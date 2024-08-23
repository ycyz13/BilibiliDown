package ycyz;

import org.junit.Test;
import ycyz.client.DownloadArgs;
import ycyz.client.DownloadService;

import java.time.LocalDateTime;

public class DownloadTest {
    @Test
    public void testDownload(){
        DownloadService service = new DownloadService();
        service.login();
        service.downloazdByPage(DownloadArgs.builder().uid("2615982").startTime(LocalDateTime.
                parse("2022-01-01T10:30:59")).build());
    }
}
