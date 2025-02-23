package ycyz.client;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import nicelee.bilibili.INeedAV;
import nicelee.bilibili.INeedLogin;
import nicelee.bilibili.enums.VideoQualityEnum;
import nicelee.bilibili.exceptions.BilibiliError;
import nicelee.bilibili.exceptions.ChargeException;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.FavList;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.*;
import nicelee.ui.Global;
import nicelee.ui.thread.CookieRefreshThread;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ycyz.entity.Up;
import ycyz.entity.Vedio;
import ycyz.service.IUpService;
import ycyz.service.IVedioService;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DownloadService {
    private static String UP_ALL_VIDEO_URL_PREFIX =  "https://space.bilibili.com/";
    private static String BV_PREFIX = "https://www.bilibili.com/video/";

    String illegalCharsRegex = "[\\\\/:*?\"<>|]";

    @Autowired
    private IVedioService vedioService;
    @Autowired
    private IUpService upService;

    @PostConstruct
    public void init(){
        ConfigUtil.initConfigs();
    }

    public int downloazdByPage(DownloadArgs args) {
        String url = null;
        switch (args.getUrlType()) {
            case UP:
                url = UP_ALL_VIDEO_URL_PREFIX + args.getUid();
                break;
            case COLLECTION:
                url = args.getUrl();
                break;
            default:
                break;
        }

        INeedAV iNeedAV = new INeedAV();
        String avUrlNoPage = iNeedAV.getValidID(url);
        log.info("当前解析的avUrl为：" + avUrlNoPage);

        boolean stopFlag = false;
        int downloadCount = 0;
        int page = 1;

        while (!stopFlag) {
            String avUrl = url + " p=" + page++;
            VideoInfo avInfo =iNeedAV.getVideoDetail(avUrl, Global.downloadFormat, true);
            if (avInfo == null || avInfo.getClips() == null || avInfo.getClips().size() == 0) {
                break;
            }

            Map<String, Integer> bvCountMap = avInfo.getClips().values().stream().collect(Collectors.toMap(ClipInfo::getAvId, c -> 1, Integer::sum));

            for (ClipInfo clipInfo : avInfo.getClips().values()) {
                if (args.getStartTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond()*1000 >= clipInfo.getcTime()) {
                    log.info(clipInfo.getTitle() + "在上次更新日期之前, " + clipInfo.getUpName() + "更新完毕");
                    stopFlag = true;
                    break;
                }
                log.debug(new Gson().toJson(clipInfo));
                Vedio vedio = vedioService.lambdaQuery().eq(Vedio::getBvId, clipInfo.getAvId()).eq(Vedio::getCid, String.valueOf(clipInfo.getcId()))
                        .one();
                if (vedio != null) {
                    if (vedio.getHandleStatus() == 2) {
                        log.info("该视频已经下载过，不重复处理: " + new Gson().toJson(clipInfo));
                    } else if (vedio.getHandleStatus() == 1) {
                        log.warn("该视频在其他线程处理，或等待定时任务补齐 " + new Gson().toJson(clipInfo));
                    }
                } else {
                    LocalDateTime bvCreateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(clipInfo.getcTime()), ZoneId.systemDefault());
                    vedio = Vedio.builder().bvId(clipInfo.getAvId()).cid(String.valueOf(clipInfo.getcId())).avTitle(clipInfo.getAvTitle())
                            .title(clipInfo.getTitle()).upUid(args.getUid()).handleStatus(1).bvPage(clipInfo.getPage())
                            .bvCreateTime(bvCreateTime).fileName("").storePath("").qn(-1)
                            .build();
                    vedioService.save(vedio);
                    try {
                        String urlQuery = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLink(clipInfo.getAvId(), String.valueOf(clipInfo.getcId()), 127, Global.downloadFormat); //该步含网络查询， 可能较为耗时
                        int realQN = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLinkQN();
                        // 更新qn
                        vedioService.update(null, new UpdateWrapper<Vedio>().lambda().eq(Vedio::getId, vedio.getId())
                                .set(Vedio::getQn, realQN));
                        boolean downloadResult = iNeedAV.downloadClip(urlQuery, clipInfo.getAvId(), realQN, clipInfo.getPage());
                        if (!downloadResult) {
                            log.error("该视频在其他线程处理，或等待定时任务补齐 " + new Gson().toJson(clipInfo));
                            continue;
                        }

                        boolean multiCV = bvCountMap.get(clipInfo.getAvId()) > 1;
                        Path targetFile = moveAndRename(args, clipInfo, realQN, multiCV);

                        downloadCount++;
                        vedio.setHandleStatus(2);
                        vedioService.update(null, new UpdateWrapper<Vedio>().lambda().eq(Vedio::getId, vedio.getId())
                                .set(Vedio::getHandleStatus, 2).set(Vedio::getFileName, targetFile.getFileName().toString())
                                .set(Vedio::getStorePath, targetFile.toAbsolutePath().toString())
                        );
                    } catch (ChargeException e1) {
                        vedio.setHandleStatus(3);
                        vedioService.updateById(vedio);
                        log.warn(vedio.getAvTitle() + "-----" + e1.getMessage());
                    } catch (Exception e) {
                        log.error(String.format("下载处理失败, vedio id: %s", vedio.getId()), e);
                    }
                }

                try {
                    Thread.sleep(5000); // 每个下载间隔5s
                } catch (Exception e) {
                    log.error("sleep异常", e);
                }
            }
        }

        return downloadCount;
    }

    public void downloadByBv(Vedio vedio){
        String bvId = vedio.getBvId();
        String url = BV_PREFIX + bvId;
        INeedAV iNeedAV = new INeedAV();

        VideoInfo avInfo =iNeedAV.getVideoDetail(url, Global.downloadFormat, true);
        boolean multiCV = avInfo.getClips().size() > 1;
        for (ClipInfo clipInfo : avInfo.getClips().values()) {
            if (StringUtils.equals(vedio.getBvId(), clipInfo.getAvId()) && StringUtils.equals(vedio.getCid(), String.valueOf(clipInfo.getcId()))) {
                log.info(String.format("视频匹配，进行补充下载. vedio id: %s", vedio.getId()));
            } else {
                log.info(String.format("视频不匹配，continue. vedio id: %s", vedio.getId()));
                continue;
            }
            log.info(String.format("bvid: %s, cid: %d, downloadUrl: %s", clipInfo.getAvId(), clipInfo.getcId(), new Gson().toJson(clipInfo.getLinks())));

            try {
                String urlQuery = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLink(clipInfo.getAvId(), String.valueOf(clipInfo.getcId()), 127, Global.downloadFormat); //该步含网络查询， 可能较为耗时
                int realQN = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLinkQN();
                // 更新qn
                vedioService.update(null, new UpdateWrapper<Vedio>().lambda().eq(Vedio::getId, vedio.getId())
                        .set(Vedio::getQn, realQN));
                boolean downloadResult = iNeedAV.downloadClip(urlQuery, clipInfo.getAvId(), realQN, clipInfo.getPage());
                if (!downloadResult) {
                    log.error("下载失败 " + new Gson().toJson(clipInfo));
                    return;
                }

                Up up = upService.getOne(new LambdaUpdateWrapper<Up>().eq(Up::getUid, vedio.getUpUid()));
                DownloadArgs args = DownloadArgs.builder().uid(vedio.getUpUid()).upName(up.getUpName()).bizNo(up.getBizNo()).build();
                Path targetFile = moveAndRename(args, clipInfo, realQN, multiCV);

                vedio.setHandleStatus(2);
                vedioService.update(null, new UpdateWrapper<Vedio>().lambda().eq(Vedio::getId, vedio.getId())
                        .set(Vedio::getHandleStatus, 2).set(Vedio::getFileName, targetFile.getFileName().toString())
                        .set(Vedio::getStorePath, targetFile.toAbsolutePath().toString())
                );
            } catch (ChargeException e1) {
                vedio.setHandleStatus(3);
                vedioService.updateById(vedio);
                log.warn(vedio.getAvTitle() + "-----" + e1.getMessage());
            }catch (Exception e) {
                throw new BilibiliError(String.format("下载处理失败, vedio id: %s", vedio.getId()), e);
            }

            try {
                Thread.sleep(5000); // 每个下载间隔5s
            } catch (Exception e) {
                log.error("sleep异常", e);
            }
        }

    }

    private Path moveAndRename(DownloadArgs args, ClipInfo clipInfo, int realQN, boolean multiCV){
        String avTitle = clipInfo.getAvTitle();
        String bvId = clipInfo.getAvId();
        LocalDateTime bvCreateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(clipInfo.getcTime()), ZoneId.systemDefault());

        // 原始名称：[bId]-[qutity]-[pn]
        String originName = String.format("%s-%d-p%d.mp4", bvId, realQN, clipInfo.getPage());
        String targetName = "";
        String date = bvCreateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (!multiCV) {
            // 2024-01-01_[up名称]_[标题]_[清晰度].mp4
            targetName = String.format("%s-%s-%s-%s.mp4", date, args.getUpName(), avTitle, VideoQualityEnum.getQualityDescript(realQN));
        } else {
            // 2024-01-01_[up名称]_[标题]_[p0]_[子标题]_[清晰度].mp4
            targetName = String.format("%s-%s-%s-p%d-%s-%s.mp4", date, args.getUpName(), avTitle, clipInfo.getPage()
                    ,clipInfo.getTitle(), VideoQualityEnum.getQualityDescript(realQN));
        }

        // 去掉文件名中的非法字符
        targetName = targetName.replaceAll(illegalCharsRegex, "");

        String rootPath = "./B站小姐姐/";
        String upDirPath = rootPath + args.getBizNo() + "-" + args.getUpName() + "/";
        File upDir = new File(upDirPath);
        if (!upDir.exists()) {
            upDir.mkdirs();
        }
        File originFile = new File(Global.savePath + originName);
        File targetFile = new File(upDirPath + targetName);
//        boolean renameResult = originFile.renameTo(targetFile);
        try{

            Path newPath = Files.move(Paths.get(originFile.getAbsolutePath()), Paths.get(targetFile.getAbsolutePath()), StandardCopyOption.ATOMIC_MOVE);
            log.info(String.format("移动成功: %s", newPath.getFileName()));
            return newPath;
        } catch (Exception e) {
            throw new BilibiliError(String.format("移动文件失败。old：%s, target: %s  ", originFile.getAbsolutePath(), targetFile.getAbsolutePath()), e);
        }
    }

    public void login(){
        // 尝试刷新cookie
        INeedLogin inl = new INeedLogin();
        String cookiesStr = inl.readCookies();
        if (cookiesStr != null) {
            Global.needToLogin = true;
            if(Global.tryRefreshCookieOnStartup && !Global.runWASMinBrowser) {
                HttpCookies.setGlobalCookies(HttpCookies.convertCookies(cookiesStr));
                CookieRefreshThread.showTips = false;
                CookieRefreshThread thCR = CookieRefreshThread.newInstance();
                thCR.start();
                try {
                    thCR.join();
                } catch (InterruptedException e1) {
                }
                CookieRefreshThread.showTips = true;
            }
        }

        cookiesStr = inl.readCookies();
        // 检查有没有本地cookie配置
        if (cookiesStr != null) {
            log.info("检查到存在本地Cookies...");
            List<HttpCookie> cookies = HttpCookies.convertCookies(cookiesStr);
            // 成功登录后即返回,不再进行二维码扫码工作
            if (inl.getLoginStatus(cookies)) {
                log.info("本地Cookies验证有效...");
                // 设置全局Cookie
                HttpCookies.setGlobalCookies(cookies);
                // 初始化用户数据显示
//                initUserInfo(inl);
                log.info("成功登录...");
                Global.isLogin = true;
            } else {
                log.info("本地Cookies验证无效...");
                // 置空全局Cookie
                HttpCookies.setGlobalCookies(null);
                throw new BilibiliError("登录失败，请检查cookie: " + cookiesStr);
            }
        }
    }

    public void initUserInfo(INeedLogin inl) {
        // 设置当前头像
        try {
            // System.out.println(inl.user.getPoster());
            URL fileURL = new URL(inl.user.getPoster());
            ImageIcon imag1 = new ImageIcon(fileURL);
            imag1 = new ImageIcon(imag1.getImage().getScaledInstance(80, 80, Image.SCALE_DEFAULT));
            Global.index.jlHeader.setToolTipText("当前用户为: " + inl.user.getName());
            Global.index.jlHeader.setIcon(imag1);
            // Global.index.jlHeader.removeMouseListener(Global.index);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // 设置收藏夹
        try {
            String favUrl = "https://api.bilibili.com/medialist/gateway/base/created?pn=1&ps=100&is_space=0&jsonp=jsonp&up_mid="
                    + inl.user.getUid();
            HttpRequestUtil util = new HttpRequestUtil();
            String jsonStr = util.getContent(favUrl, new HttpHeaders().getAllFavListHeaders(inl.user.getUid()),
                    HttpCookies.getGlobalCookies());
//			System.out.println(favUrl);
//			System.out.println(jsonStr);
            JSONArray list = new JSONObject(jsonStr).getJSONObject("data").getJSONArray("list");
            if (Global.index.cmbFavList.getItemCount() == 1) {
                Global.index.cmbFavList.addItem("稍后再看");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject favlist = list.getJSONObject(i);
                    FavList fav = new FavList(favlist.getLong("mid"), favlist.getLong("id"),
                            favlist.getInt("media_count"), favlist.getString("title"));
                    Global.index.cmbFavList.addItem(fav);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
