package cn.penidea.alice.ws;

import cn.penidea.alice.entity.Friend;
import cn.penidea.alice.entity.Message;
import cn.penidea.alice.entity.Params;
import cn.penidea.alice.entity.Request;
import cn.penidea.alice.service.impl.ChatGPTServiceImpl;
import cn.penidea.alice.thread.ReConnectTask;
import cn.penidea.alice.util.BaseConfigBean;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * 功能:监听类
 * 作者:Mr.Fokers
 * 日期：2022年09月28日 9:37
 */

@ClientEndpoint
@Component
@Slf4j
public class Client {

    private Session session;
    private static Client INSTANCE;
    private static ChatGPTServiceImpl chatGPTService;
    private static boolean connecting = false;
    private static String MASTER = null;
    private static BaseConfigBean baseConfigBean;


    @Autowired
    public void setChatGPTService(ChatGPTServiceImpl chatGPTService) {
        Client.chatGPTService = chatGPTService;
    }

    @Autowired
    public void setBaseConfigBean(BaseConfigBean baseConfigBean) {
        Client.baseConfigBean = baseConfigBean;
    }


    private Client() {
    }

    private Client(String url) throws DeploymentException, IOException {
        session = ContainerProvider.getWebSocketContainer().connectToServer(this, URI.create(url));
    }

    public synchronized static boolean connect(String url) {
        try {
            INSTANCE = new Client(url);
            connecting = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("连接失败");
            return false;
        }
    }

    public synchronized static void reConnect() {
        if (!connecting) {
            connecting = true;
            if (INSTANCE != null) {
                INSTANCE.session = null;
                INSTANCE = null;
            }
        }
        ReConnectTask.execute();
    }

    @OnOpen
    public void onOpen(Session session) {
        log.info("消息监听开启成功");
    }

    @OnMessage
    public void onMessage(String json) {
        Message message = JSONObject.parseObject(json, Message.class);
        String msg = message.getMessage();
        String postType = message.getPost_type();
        if (postType != null && postType.equals("message")) {
            try {
                filterFunction(message);
            } catch (IOException e) {
                sendMessage(baseConfigBean.getUserList().get("admin"), null, "private", "Alice Error in" + new Date(), false);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("服务已关闭");
        reConnect();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("服务异常断开");
        throwable.printStackTrace();
        reConnect();
    }

    public static void sendMessage(String json) {
        Client.INSTANCE.session.getAsyncRemote().sendText(json);
    }

    public static void sendMessage(String qq, String groupId, String type, String msg, boolean escape) {
        Request<Params> request = new Request<>();
        request.setAction("send_msg");
        Params params = new Params();
        params.setUser_id(qq);
        params.setGroup_id(groupId);
        params.setMessage(msg);
        params.setAuto_escape(escape);
        params.setMessage_type(type);
        request.setParams(params);
        sendMessage(JSON.toJSONString(request));
    }


    public static void filterFunction(Message message) throws IOException {
        String messageType = message.getMessage_type(); //消息类型
        String msg = message.getMessage(); //消息内容
        String from = message.getUser_id();//发送方qq
        String groupId = message.getGroup_id();//群聊号
        if (baseConfigBean.getUserList().get(from) != null || baseConfigBean.isPublic == "true") {
            if (msg.contains(baseConfigBean.getAtRobotCQ() + " 问个好")) {
                sendMessage(from, groupId, messageType, "你以为你是谁，随随便便就想让我给大家问好？", false);
                return;
            } else if (MASTER == null || baseConfigBean.getUserList().get("admin").equals(from)) {
                if ((msg.equals(baseConfigBean.getWakeUpWord()) || msg.startsWith(baseConfigBean.getAtRobotCQ()))) {
                    MASTER = from;
                    if (msg.equals(baseConfigBean.getAtRobotCQ()) || msg.equals(baseConfigBean.getAtRobotCQ() + " ") || msg.equals(baseConfigBean.getWakeUpWord())) {
                        sendMessage(from, groupId, messageType, baseConfigBean.getPromptUpWord(), false);
                    }
                }
            }
            
            if (MASTER != null){
            	if (from.equals(MASTER)) {
            		if (msg.startsWith(baseConfigBean.getAtRobotCQ()) && !msg.equals(baseConfigBean.getAtRobotCQ()) && !msg.equals(baseConfigBean.getAtRobotCQ() + " ")) {
            			if (msg.equals(baseConfigBean.getAtRobotCQ() + " ")) {
            				msg = msg.substring(msg.indexOf("]") + 3);
            			} else {
            				msg = msg.substring(msg.indexOf("]") + 2);
            			}
            		}
            		if (msg.equals(baseConfigBean.getStandbyWord())) {
                        MASTER = null;
                        sendMessage(from, groupId, messageType, baseConfigBean.getStandbyPrompt(), false);
                    }
            		if (baseConfigBean.getUserList().get("admin").equals(from)) {
            			if (msg.startsWith("add [CQ:at,qq=")) {
            				String qq = msg.substring(msg.indexOf("也和这位聊吧[CQ:at,qq=") + 16, msg.indexOf("]"));
            				baseConfigBean.getUserList().put(qq, "");
            				sendMessage(from, groupId, messageType, "要让我和" + qq + "聊啊，行吧！", false);
            				return;
            			} else if (msg.startsWith("别理[CQ:at,qq=")) {
            				String qq = msg.substring(msg.indexOf("别理[CQ:at,qq=") + 12, msg.indexOf("]"));
            				if (baseConfigBean.getUserList().get(qq) != null) {
            					baseConfigBean.getUserList().remove(qq);
            					sendMessage(from, groupId, messageType, "好吧，那我再也不理" + qq + "了", false);
            				} else {
            					sendMessage(from, groupId, messageType, "哼哼，我早就不理他了", false);
            				}
            				return;
            			} else if (msg.equals("#设为公有")) {
            				baseConfigBean.isPublic = "true";
            				sendMessage(from, groupId, messageType, "想让我和大家都聊天？好吧...", false);
            				return;
            			} else if (msg.equals("#设为私有")) {
            				baseConfigBean.isPublic = "false";
            				sendMessage(from, groupId, messageType, "哼哼，我现在只和我认识的人聊天了", false);
            				return;
            			} else if (msg.equals("#设为半公有")) {
            				baseConfigBean.isPublic = "pubvate";
            				sendMessage(from, groupId, messageType, "想和我聊天，求我啊？", false);
            				return;
            			} else if (msg.equals("#重置会话")) {
            				chatGPTService.reset();
            				sendMessage(from, groupId, messageType, "啊啊啊，我聊天的记忆，正在消失...", false);
            				sendMessage(from, groupId, messageType, baseConfigBean.getPromptUpWord(), false);
            				return;
            			}
            		}
            		try {
            			if (!msg.equals(baseConfigBean.getAtRobotCQ()) && !msg.equals(baseConfigBean.getAtRobotCQ() + " ") && !msg.equals(baseConfigBean.getWakeUpWord())) {
            				sendMessage(from, groupId, messageType, baseConfigBean.getLoadingWord(), false);
            				String answer = chatGPTService.askQuestion(msg);
            				answer = answer.replace("Assistant", baseConfigBean.getRobotName());
            				sendMessage(from, groupId, messageType, answer, false);
            				MASTER = null;
            			}
            		} catch (Exception e) {
            			chatGPTService.refresh();
            			}
            	} else {
            		if ((msg.equals(baseConfigBean.getWakeUpWord()) || msg.startsWith(baseConfigBean.getAtRobotCQ()))) {
                		sendMessage(from, groupId, messageType, "别吵吵，我还在和" + MASTER + "聊天呢，等下哈", false);
                	}
                  }
            }
        } else if (msg.contains(baseConfigBean.getAtRobotCQ() + " 也和我聊聊呗") && baseConfigBean.isPublic == "pubvate"){
        	baseConfigBean.getUserList().put(from, "");
        	if (from.equals(MASTER)) {
            sendMessage(from, groupId, messageType, "行行行，我也可以和你聊天", false);
        	} else {
        		sendMessage(from, groupId, messageType, "行，等我和" + MASTER + "聊完再跟你聊天", false);
        	}
            return;
        }
    }
}
