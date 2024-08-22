package ycyz.client;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import nicelee.bilibili.INeedAV;
import nicelee.bilibili.INeedLogin;
import nicelee.bilibili.model.ClipInfo;
import nicelee.bilibili.model.FavList;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.*;
import nicelee.ui.Global;
import nicelee.ui.thread.CookieRefreshThread;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
public class DownloadService {
    public void downloazdByPage(DownloadArgs args) {
        // 初始化配置 TODO：系统启动时加载
        ConfigUtil.initConfigs();

        INeedAV iNeedAV = new INeedAV();
        String avId = iNeedAV.getValidID(args.getUrl());
        log.info("当前解析的id为：" + avId);



        VideoInfo avInfo =iNeedAV.getVideoDetail(avId, Global.downloadFormat, true);

        for (ClipInfo clipInfo : avInfo.getClips().values()) {
            log.info(new Gson().toJson(clipInfo));
            String urlQuery = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLink(clipInfo.getAvId(), String.valueOf(clipInfo.getcId()), 127, Global.downloadFormat); //该步含网络查询， 可能较为耗时
            int realQN = iNeedAV.getInputParser(clipInfo.getAvId()).getVideoLinkQN();
            iNeedAV.downloadClip(urlQuery, clipInfo.getAvId(), realQN, clipInfo.getPage());
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
            System.out.println("检查到存在本地Cookies...");
            List<HttpCookie> cookies = HttpCookies.convertCookies(cookiesStr);
            // 成功登录后即返回,不再进行二维码扫码工作
            if (inl.getLoginStatus(cookies)) {
                System.out.println("本地Cookies验证有效...");
                // 设置全局Cookie
                HttpCookies.setGlobalCookies(cookies);
                // 初始化用户数据显示
//                initUserInfo(inl);
                System.out.println("成功登录...");
                Global.isLogin = true;
            } else {
                System.out.println("本地Cookies验证无效...");
                // 置空全局Cookie
                HttpCookies.setGlobalCookies(null);
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
