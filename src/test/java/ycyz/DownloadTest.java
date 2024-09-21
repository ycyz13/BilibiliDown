package ycyz;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ycyz.client.DownloadArgs;
import ycyz.client.DownloadService;

import java.time.LocalDateTime;


// 请写一个springboot的测试类
@SpringBootTest
@RunWith(SpringRunner.class)
public class DownloadTest {
    @Autowired
    private DownloadService downloadService;

    @Test
    public void testDownload(){
        downloadService.login();
        downloadService.downloazdByPage(DownloadArgs.builder().uid("2615982").startTime(LocalDateTime.
                parse("2024-09-11T10:30:59")).upName("测试up").build());
    }

    @Test
    public void testDownloadByBvId(){
        downloadService.login();
//        downloadService.downloadByBv("BV1MD4y1c7np");

    }
}
