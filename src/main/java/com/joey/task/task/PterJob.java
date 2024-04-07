package com.joey.task.task;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.joey.task.util.Constants;
import com.joey.task.util.DateUtils;
import com.joey.task.util.QbittorrentUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping
public class PterJob  {

    @Value("${pter.url}")
    private String pterUrl;

    @Value("${pter.cookie}")
    private String pterCookie;

    @Value("${pter.tag}")
    private String pterTag;

    @Value("${pter.minSeeder}")
    private Integer minSeeder;


    @Value("${pter.maxSeeder}")
    private Integer maxSeeder;

    @Value("${pter.maxTorrentStopCnt}")
    private Integer maxTorrentStopCnt;

    @Autowired
    private QbittorrentUtil qbittorrentUtil;


    private AtomicBoolean continueFlag = new AtomicBoolean(true);

    private Integer pageNum;


    //当前符合下载6人种个数
    private int curTorrentCnt = 0;

    //最大种子大小
    @Value("${pter.maxDefendSize}")
    private int maxTorrentSize ;
    private int minTorrentSize = 100;
    //
    private int sevenTorrentCnt = 0;
    private int maxSevenTorrentStopCnt = 400;


    @RequestMapping("/spider")
    @Scheduled(cron = "${quartz.cron}")
    public void spiderPter() {
        long begin = System.currentTimeMillis();
        continueFlag.set(true);
        pageNum = 0;
        String sid = qbittorrentUtil.login();
        boolean chooseDate = false;
        do {
            log.info("当前页数:" + pageNum);
            HttpResponse response = HttpUtil.createGet(pterUrl + pageNum).header("cookie", pterCookie).header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36").execute();
            if (response.getStatus() == 200) {
                String body = response.body();
                log.info("body:{}", body);
                List<String> torUrlList = parseHtml(body, chooseDate);
                for (String torUrl : torUrlList) {
                    try {
                        boolean stopFlag = qbittorrentUtil.addTorrents(torUrl, sid, pterTag, continueFlag);
                        if (!stopFlag) {
                            break;
                        }
                    } catch (Exception e) {
                        log.error("异常: " + e.getMessage());
                    }
                    int rdm = new Random().nextInt(500);
                    try {
                        Thread.sleep(1000 + rdm);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            pageNum++;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (continueFlag.get());
        long end = System.currentTimeMillis();

        log.info("----下载种子完成----, 共耗时:" + (end - begin) / 1000 + "s");
    }


    public List<String> parseHtml(String html, boolean chooseDate) {
        Document parse = Jsoup.parse(html, "utf-8");
        List<String> list = new ArrayList<>();
        Element elementById = parse.getElementById("torrenttable");
        Elements elementsByClass = elementById.getElementsByTag("tr");
        if (elementsByClass.size() < 5) {
            continueFlag.set(false);
            return list;
        }
        for (Element element : elementsByClass) {
            Elements tdEls = element.getElementsByTag("td");
            if (tdEls.size() != 16) {
                continue;
            }
            int seeder = Integer.parseInt(tdEls.get(12).text());
            int leecher = Integer.parseInt(tdEls.get(13).text());
            //过滤置顶
            int isZhidingSize = tdEls.get(3).getElementsByTag("img").stream().filter(e -> e.attr("title").contains("置顶")).collect(Collectors.toList()).size();
            if (isZhidingSize > 0) {
                continue;
            }
            String ttlDateStr = tdEls.get(10).getElementsByTag("span").attr("title");
            if (chooseDate && DateUtils.differentDaysByMillisecond(new Date(), DateUtils.parseDate(ttlDateStr)) < 380) {
                continue;
            }
            //做种+下载人数在2-5人
            if (seeder >= minSeeder && seeder + leecher <= maxSeeder) {
                String sizeStr = tdEls.get(11).text();
//                float torSize = new BigDecimal(sizeStr.split(" ")[0]).floatValue();
                float torSize = convertMB(sizeStr);
                //限制大小
                if (torSize > minTorrentSize && torSize < maxTorrentSize) {
                    String dlUrlStr = "https://pterclub.com/" + tdEls.get(6).getElementsByTag("a").get(0).attr("href");

                    if (!tdEls.get(3).getElementsByTag("img").hasClass("progbargreen") && curTorrentCnt < maxTorrentStopCnt) {
                        log.info("种子链接: " + dlUrlStr);
                        list.add(dlUrlStr);
                        curTorrentCnt++;
                    }
                    if (curTorrentCnt >= maxTorrentStopCnt) {
                        log.info(maxSeeder + "人种下满" + maxTorrentStopCnt + "个了");
                        continueFlag.set(false);
                        return list;
                    }
                }
            }
            //做种人数大于等于7人
            else if (seeder > maxSeeder) {
                sevenTorrentCnt++;
            }
            if (sevenTorrentCnt > maxSevenTorrentStopCnt) {
                log.info("没有" + maxSeeder + "人种了");
                continueFlag.set(false);
                return list;
            }
        }
        return list;
    }

    /**
     * 单位转化
     *
     * @return
     */
    private float convertMB(String sizeStr) {
        float torSize = new BigDecimal(sizeStr.split(" ")[0]).floatValue();
        if (sizeStr.endsWith("MB")) {
            return torSize;
        } else if (sizeStr.endsWith("GB")) {
            return torSize * 1024;
        }
        return 0f;
    }


}
