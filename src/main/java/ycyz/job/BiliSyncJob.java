package ycyz.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ycyz.client.DownloadArgs;
import ycyz.client.DownloadService;
import ycyz.entity.Up;
import ycyz.entity.Vedio;
import ycyz.service.IUpService;
import ycyz.service.IVedioService;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class BiliSyncJob {
    @Autowired
    private DownloadService downloadService;
    @Autowired
    private IUpService upService;
    @Autowired
    private IVedioService vedioService;
    /**
     * 启动后延迟30秒开始
     * 两次同步间隔30分钟
     */
    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 5 * 60 * 1000)
    public void syncUpVedio(){
        log.info(String.format("开始同步up投稿"));
        List<Up> ups = upService.lambdaQuery().orderByAsc(Up::getLastSyncTime).list();
        downloadService.login();
        int totalCount = 0;
        for (Up up : ups) {
            try {
                LocalDateTime now = LocalDateTime.now();
                DownloadArgs args = DownloadArgs.builder().uid(up.getUid()).bizNo(up.getBizNo()).upName(up.getUpName())
                        .startTime(up.getLastSyncTime()).build();
                int count = downloadService.downloazdByPage(args);
                totalCount += count;
                log.info(up.getUpName() + "更新结束, 下载投稿数量: " + count);
                up.setLastSyncTime(now);
                up.setUpdateTime(now);
                upService.updateById(up);
                Thread.sleep(30000);  // 每个up间隔30s
            } catch (Exception e) {
                log.error("下载失败: " + new Gson().toJson(up), e);
            }
        }
        log.info(String.format("本轮下载完毕.up总数: %d, 总投稿数: %d", ups.size(), totalCount));
        // todo: 所有up更新完毕，发送统计邮。可单开job每日22点发送一次。
    }

//    @Scheduled(initialDelay = 15 * 1000, fixedDelay = 2 * 60 * 1000)
    public void syncErrorVedio(){
        log.info(String.format("开始同步异常vedio"));
        // 同步两小时前处理的异常视频
        List<Vedio> vedios = vedioService.list(new LambdaQueryWrapper<Vedio>().eq(Vedio::getHandleStatus, 1)
                .le(Vedio::getUpdateTime, LocalDateTime.now().plusHours(-2)));
        log.info(String.format("需要同步的异常vedio数量: %d", vedios.size()));
        downloadService.login();
        int totalCount = 0;
        for (Vedio vedio : vedios) {
            try {
                downloadService.downloadByBv(vedio);
                totalCount += 1;
                Thread.sleep(5000);  // 每个vedio间隔5秒
            } catch (Exception e) {
                log.error("补偿下载失败: " + new Gson().toJson(vedio), e);
            }
        }
        log.info(String.format("同步异常vedio END, count: %d", totalCount));
    }
}
