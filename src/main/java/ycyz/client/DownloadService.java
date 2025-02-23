package ycyz.client;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import nicelee.bilibili.INeedAV;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.ConfigUtil;
import nicelee.bilibili.util.Logger;
import nicelee.ui.Global;
import org.json.JSONObject;

import java.util.Map;

@Slf4j
public class DownloadService {
    public void downloazdByPage(DownloadArgs args) {
        // 初始化配置 TODO：系统启动时加载
        ConfigUtil.initConfigs();

        INeedAV iNeedAV = new INeedAV();
        String avId = iNeedAV.getValidID(args.getUrl());
        log.info("当前解析的id为：" + avId);

        VideoInfo avInfo =iNeedAV.getVideoDetail(avId, Global.downloadFormat, false);
        for (ClipInfo clipInfo : avInfo.getClips().values()) {
            log.info(new Gson().toJson(clipInfo));
            iNeedAV.downloadClip(clipInfo.getLinks().get(0), avId, iNeedAV.getInputParser(avId).getVideoLinkQN(), clipInfo.getPage());
        }
    }
}
