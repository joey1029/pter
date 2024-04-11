package com.joey.task.task;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class PterDelMsgJob {


    @Value("${pter.cookie}")
    private String pterCookie;


    @RequestMapping("/delPterMsg")
    @Scheduled(cron = "${quartz.delptermsgcron}")
    public void delPterMsg() {
        long begin = System.currentTimeMillis();
        HttpResponse response = HttpUtil.createGet("https://pterclub.com/messages.php").header("cookie", pterCookie).header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").execute();
        if (response.getStatus() == 200) {
            String body = response.body();
            log.info("body:{}", body);
            JXDocument jxDocument = JXDocument.create(body);
            List<JXNode> jxNodes = jxDocument.selN("//*[@id=\"outer\"]/form/table/tbody/tr");
            for (JXNode jxNode : jxNodes) {
                JXNode node = jxNode.selOne("//td[2]/a/allText()");
                if (node != null && (node.asString().contains("您正在下载或做种的种子") || node.asString().contains("您正在下載或做種的種子"))) {
                    String msgId = jxNode.selOne("//td[5]/input/@value").asString();
                    HttpResponse response1 = HttpUtil.createGet("https://pterclub.com/messages.php?action=deletemessage&id=" + msgId).header("cookie", pterCookie).header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").execute();
                    System.out.println();
                }
            }
        }
        long end = System.currentTimeMillis();

        log.info("----删除猫站正在下载或做种的种子----, 共耗时:" + (end - begin) / 1000 + "s");
    }

}
