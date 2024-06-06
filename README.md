# 保种

## 1.自动下种
* qbittorrent只支持4.1及以上版本
* 测试抓取数据
  请求：http://xxx:29223/spider
* [application.yml](https://github.com/joey1029/pter/blob/main/src/main/resources/application.yml)需要修改的的配置
```
...

pter:
    # 官种页面（带排序参数,按照做种人数升序）
    url: https://pterclub.com/officialgroup.php?inclbookmarked=0&incldead=1&spstate=2&tag_internal=yes&sort=7&type=asc&page=
    # cookie【需要修改】
    cookie: xxxx
    # 单种最大大小（MB）【需要修改】
    maxDefendSize: 5000
    # qb保种标签
    tag: pter
    # 单种最小保种人数【需要修改】
    minSeeder: 1
    # 单种最大保种人数【需要修改】
    maxSeeder: 5
    # 猫站4小时流控数量【需要修改】
    maxTorrentStopCnt: 80
    # 开始页数【需要修改】
    startPage: 1

tr:
  # tr地址【需要修改】
  url: http://192.168.3.10:9091
  # 用户名【需要修改】
  username: admin
  # 密码【需要修改】
  password: admin
  # 错误信息包含（多个用分号分割）【需要修改】
  errorMsg: err torrent banned;torrent does not exist

quartz:
    # pterclub定时任务
    cron: 0 40 0/2 * * ?
    # 删除站内消息
    delptermsgcron: 0 10 15 * * ?
    # 删除tr ban种子
    trdelbancron: 0 10 5 * * ?
...
```


##
* [docker-compose](https://github.com/joey1029/pter/blob/main/docker-compose.yaml)修改 

```
version: '3'
services:
  defend_cat:
    ...
    # 此处需要修改路径 左侧是application.yml真实路径
    volumes:
      - 【修改后的application.yml文件路径】:/application.yml
    ...
    
  # 如果没有redis，则需要添加redis
  redis:
    image: redis
    restart: always
    container_name: redis
    ports:
      - 6379:6379
    network_mode: bridge
```

