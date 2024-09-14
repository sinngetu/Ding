package com.zuanpin.ding;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Message {
    public static final String PIPE_NAME = "BigNews";

    public static void main(String[] args) throws IOException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        executorService.scheduleAtFixedRate(new Server(8151), 0, 1, TimeUnit.SECONDS);

        // try {
        //     // create named pipe and a thread pool for single thread
        //     ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        //     RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\" + PIPE_NAME, "r");

        //     executorService.scheduleAtFixedRate(new Task(pipe), 0, 1, TimeUnit.SECONDS);
        // } catch (IOException e) {
        //     throw new RuntimeException(e);
        // }
    }
}

final class Sender {
    private static final String TOKEN = "";
    private static final String SECRET = "";

    public static boolean send(String message) {
        try {
            JSONArray newsList = new JSONArray(message);

            // get req args
            Long timestamp = System.currentTimeMillis();
            System.out.println(timestamp);
            String secret = SECRET;
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)),"UTF-8");

            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?sign="+sign+"&timestamp="+timestamp);

            for (int i = 0; i < newsList.length(); i++) {
                JSONObject news = newsList.getJSONObject(i);
                String link = news.getString("link");
                String title = news.getString("title");
                String translation = news.optString("translation");

                System.out.println("{ title: " + title + ", translation: " + translation + ", Link: " + link + " }");

                // set message conten
                OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
                String content = translation.isEmpty()
                    ? title + '\n' + link
                    : title + '\n' + translation + '\n' + link;

                text.setContent(content);

                OapiRobotSendRequest req = new OapiRobotSendRequest();
                req.setMsgtype("text");
                req.setText(text);

                OapiRobotSendResponse rsp = client.execute(req, TOKEN);
                System.out.println(rsp.getBody());
            }

            return true;
        } catch (JSONException e) {
            System.out.println("JSON exception Error: " + e.getMessage());
            return false;
        } catch (ApiException e) {
            System.out.println("API Error: " + e.getMessage());
            return false;
        } catch (UnsupportedEncodingException e) {
            System.out.println("Encoding Error: " + e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Secure Algorithm Error: " + e.getMessage());
            return false;
        } catch (InvalidKeyException e) {
            System.out.println("Secure Key Error: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("Communication Error: " + e.getMessage());
            return false;
        }
    }
}

class Task implements Runnable {
    private RandomAccessFile pipe;

    public Task(RandomAccessFile pipe) {
        super();
        this.pipe = pipe;
    }

    @Override
    public void run() {
        try {
            // read msg from pipe
            String message = pipe.readLine();

            Sender.send(new String(message.getBytes("ISO-8859-1"), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class Server implements Runnable {
    private int port;

    public Server(int port) {
        super();
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server is started");

            while(true) {
                try(Socket client = server.accept()) {
                    System.out.println(client.getInetAddress().getHostAddress() + ":" + client.getPort() + " has connected.");

                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                    String message = in.readLine();

                    Sender.send(message);
                } catch (UnsupportedEncodingException e) {
                    System.out.println("Encoding Error: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Communication Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}