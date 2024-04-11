package com.joey.task.task;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class TransmissionController {


    @Value("${tr.username}")
    private String username;

    @Value("${tr.password}")
    private String password;

    @Value("${tr.url}")
    private String transmissionUrl;

    @Value("${tr.errorMsg}")
    private String errorMsg;
    private String session_id;
    private String auth;
    private String getTorrentBody = "{\n" + "   \"arguments\": {\n" + "     \"fields\": [\n" + "       \"id\",\n" + "       \"error\",\n" + "       \"errorString\"\n" + "     ]\n" + "   },\n" + "   \"method\": \"torrent-get\"\n" + "}";


    @RequestMapping("delErrorTorrents")
    @Scheduled(cron = "${quartz.trdelbancron}")
    public String delErrorTorrents() {
        //构建授权信息
        buildAuthorization();
        //获取sesssion_id
        getSessionId();
        //获取错误种子
        List<Integer> delIds = getErrorTorrents();
        if (!CollectionUtils.isEmpty(delIds)) {
            //删除种子
            String delResult = removeTorrents(delIds, true);
            log.info("删除结果, {}", delResult);
        }
        return "删除成功";
    }


    private void buildAuthorization() {
        String authString = username + ":" + password;
        String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
        auth = encodedAuthString;
    }

    private void getSessionId() {
        HttpResponse response = HttpUtil.createGet(transmissionUrl + "/transmission/rpc").header("Authorization", "Basic " + auth).execute();
        String sessionId = response.header("X-Transmission-Session-Id");
        session_id = sessionId;
    }

    private List<Integer> getErrorTorrents() {
        List<Integer> delIds = new ArrayList<>();
        HttpRequest httpRequest = HttpUtil.createPost(transmissionUrl + "/transmission/rpc").header("Authorization", "Basic " + auth).body(getTorrentBody);
        httpRequest.header("X-Transmission-Session-Id", session_id);
        HttpResponse response = httpRequest.execute();
        if (response.getStatus() == 200) {
            String body = response.body();
            JSONObject bodyJson = JSONObject.parseObject(body);
            if ("success".equals(bodyJson.getString("result"))) {
                JSONObject argumentsJson = bodyJson.getJSONObject("arguments");
                JSONArray torrentArr = argumentsJson.getJSONArray("torrents");
                for (int i = 0; i < torrentArr.size(); i++) {
                    JSONObject torrentJson = torrentArr.getJSONObject(i);
                    Integer error = torrentJson.getInteger("error");
                    String errorString = torrentJson.getString("errorString");
                    if (StringUtils.isNotBlank(errorString)) {
                        Integer torrentId = torrentJson.getInteger("id");
                        log.info("需要删除种子id: {}, 错误信息: {}", torrentId, errorString);
                        String[] errorArr = errorMsg.split(";");
                        for (int j = 0; j < errorArr.length; j++) {
                            if (errorString.contains(errorArr[j])) {
                                delIds.add(torrentId);
                            }
                        }
                    }
                }
            }
        }
        return delIds;
    }


    private String removeTorrents(List<Integer> delIds, boolean isDelLocalData) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("method", "torrent-remove");
        JSONObject argumentsJson = new JSONObject();
        argumentsJson.put("delete-local-data", isDelLocalData);
        argumentsJson.put("ids", delIds);
        paramJson.put("arguments", argumentsJson);
        HttpRequest httpRequest = HttpUtil.createPost(transmissionUrl + "/transmission/rpc").header("Authorization", "Basic " + auth).body(paramJson.toJSONString());
        httpRequest.header("X-Transmission-Session-Id", session_id);
        HttpResponse response = httpRequest.execute();
        if (response.getStatus() == 200) {
            String body = response.body();
            JSONObject bodyJson = JSONObject.parseObject(body);
            return bodyJson.getString("result");
        }
        return null;
    }
}
