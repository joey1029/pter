# 保种

## 1.自动下种
* qbittorrent只支持4.1及以上版本 
* 启动前需要修改[application.yml](https://github.com/joey1029/pter/blob/main/src/main/resources/application.yml)文件
* 使用[docker-compose](https://github.com/joey1029/pter/blob/main/docker-compose.yaml)启动部署服务

## 2.删除错误种
* 新增自动删种功能
  请求：http://xxx:29223/delErrorTorrents
```
application.yml需要修改

tr:
  # tr地址【需要修改】
  url: http://192.168.3.10:9091
  # 用户名【需要修改】
  username: admin
  # 密码【需要修改】
  password: admin
  # 错误信息包含（多个用分号分割）【需要修改】
  errorMsg: err torrent banned;torrent does not exist
```
