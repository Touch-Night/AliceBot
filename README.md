AliceBot
===========================

###### 环境依赖
* ubuntu 18.04/22.04
* jdk8
* go-cqhttp_linux_arm64

###### 部署步骤
1. 安装jdk1.8<br>
     sudo apt-get install openjdk-8-jdk
     
2. 下载运行go-cq
      下载地址：https://docs.go-cqhttp.org/guide/quick_start.html#%E5%9F%BA%E7%A1%80%E6%95%99%E7%A8%8B<br>
      下载到服务器后使用tar -zxvf进行解压<br>
      运行其中的的在解压目录下运行screen -S gocq<br>
      再运行./go-cqhttp，第一次运行会先让你选连接方式，我们选择2：正向Websocket,gocq在解压目录下生成config.yml，这时我们先Ctrl+C<br>
      运行nano config.yml，拉到最下面找到address: 0.0.0.0:8080，把8080替换成9099,然后Ctrl+X,然后按Y，再回车一下保存<br>
      然后再次运行./go-cqhttp，完成登录后按Ctrl+A+D放到后台运行<br>
2. 执行jar包
     先输入screen -S Alice 创建个会话<br>
     然后使用java -jar -Xmx512m --clientBaseConfig.admin=管理员QQ --clientBaseConfig.robot=机器人QQ --clientBaseConfig.wakeUpWord=唤醒词 --clientBaseConfig.standbyWord=待机词 --clientBaseConfig.promptUpWord=提示词 --clientBaseConfig.robotName=机器人名称 --chatGPT.email=gpt邮箱 --chatGPT.password=gpt密码 --chatGPT.sessionToken=token --server.port=8080<br>
     运行后再使用Ctrl+A+D放到后台运行即可

###### 机器人的所有管理员操作指令（以下指令皆需唤醒机器人后再进行发送才能生效）<br>
#重置会话           重置对话<br>
#设为私有           将机器人设为私有的，需要权限(默认就是私有)<br>
#设为公有           将机器人设为所有人可用<br>
#设为半公有         将机器人设为半公有，其他人可以把自己添加到机器人的聊天对象列表中<br>
也和这位聊吧@某人    添加一个拥有权限的使用者<br>
别理@某人           将某人从使用者列表移除<br>

###### 机器人的其他操作指令（以下指令部分无需唤醒机器人后再进行发送才能生效）<br>
@机器人+问个好       让机器人问好<br>
@机器人+也和我聊聊呗 在半公有情况下把自己添加到机器人的聊天对象中<br>

###### V2.0.0 版本内容更新 <br>
1. 新功能     文件配置<br>
2. 新功能     可设置公私有化<br>
3. 新功能     当机器人在和别人聊天时能够推掉你的聊天请求了（可以告诉你它在和别人聊，让你等一等）<br>
4. 修复       手机At机器人异常的问题<br>
5. 修复       非管理员也可以正常结束对话了<br>
6. 修改       为了连续对话体验，只有发送对应的结束词才能结束对话<br>
