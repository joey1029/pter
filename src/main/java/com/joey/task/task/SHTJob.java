package com.joey.task.task;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Random;


@RestController
@Slf4j
public class SHTJob  {


    @Value("${sht.url}")
    public String baseUrl = "https://1kdj5.app/";

    @Value("${sht.cookie}")
    public String ck;
    public String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    public String fid = "103";

    @Value("${sht.replyMsgs}")
    public String replyMsgs;

    public String[] replyMsg;


    @RequestMapping("/sht")
    @Scheduled(cron = "${quartz.shtcron}")
    public void sht() {
        String[] replyMsgArr = replyMsgs.split(";");
        replyMsg = replyMsgArr;
        String forumBody = getForumHtml();
        log.info("页面:{}", forumBody);
        long days = System.currentTimeMillis() / (24 * 3600 * 1000);
        int mod = (int) (days % 5);
        if (StringUtils.isNotBlank(forumBody)) {
            int index = new Random().nextInt(replyMsg.length);
            log.info("随机评论：{}", replyMsg[index]);
            reply(fid, forumBody, replyMsg[index], mod);
        }
    }

    private String getForumHtml() {
        String url = baseUrl + "forum.php?mod=forumdisplay&fid=" + fid;
        HttpResponse response = HttpUtil.createGet(url).header("user-agent", ua).header("cookie", ck).execute();
        if (response.getStatus() == 200) {
            String body = response.body();
            System.out.println(body);
            return body;
        }
        return null;
    }


    private void reply(String fid, String html, String replyMsg, int mod) {
        JXDocument jxDocument = JXDocument.create(html);
        String formhash = jxDocument.selOne("//*[@id=\"scbar_form\"]/input[2]/@value").toString();
        List<JXNode> jxNodes = jxDocument.selN("//*[@id=\"threadlisttableid\"]/tbody/@id");
        int index = 0;
        for (int i = 0; i < jxNodes.size(); i++) {
            JXNode jxNode = jxNodes.get(i);
            if (jxNode.asString().contains("normalthread_")) {
                String tid = jxNode.asString().split("_")[1];
                index++;
                if (index == mod) {
                    String msg = doRelpy(fid, tid, formhash, replyMsg);
                    log.info("回复成功，内容：{}", replyMsg);
                    return;
                }
            }
        }
    }

    /**
     * 回复
     *
     * @param fid
     * @param tid
     * @param formhash
     * @param message
     */
    private String doRelpy(String fid, String tid, String formhash, String message) {
        try {
            String url = baseUrl + "forum.php?mod=post&infloat=yes&action=reply&fid=" + fid + "&extra=&tid=" + tid + "&replysubmit=yes&inajax=1";


            PostMethod postMethod = new PostMethod(url);
            postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            postMethod.setRequestHeader("cookie", ck);
            postMethod.setRequestHeader("user-agent", ua);
            //参数设置，需要注意的就是里边不能传NULL，要传空字符串
            NameValuePair[] data = {new NameValuePair("formhash", formhash), new NameValuePair("handlekey", "reply"), new NameValuePair("noticeauthor", ""), new NameValuePair("noticetrimstr", ""), new NameValuePair("noticeauthormsg", ""), new NameValuePair("usesig", "0"), new NameValuePair("subject", ""), new NameValuePair("message", message)};

            postMethod.setRequestBody(data);
            org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
            httpClient.executeMethod(postMethod);


            // 获取响应码
            int responseCode = postMethod.getStatusCode();
            if (responseCode == 200) {
                String result = postMethod.getResponseBodyAsString();
                // 打印响应内容
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }



}
