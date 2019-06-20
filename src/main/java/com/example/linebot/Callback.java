package com.example.linebot;



import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.action.*;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.quickreply.QuickReply;
import com.linecorp.bot.model.message.quickreply.QuickReplyItem;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@LineMessageHandler
public class Callback {


    private LineMessagingClient client;

    @Autowired
    public Callback(LineMessagingClient client) {
        this.client = client;
    }

    private static final Logger log = LoggerFactory.getLogger(Callback.class);

    // フォローイベントに対応する
    @EventMapping
    public TextMessage handleFollow(FollowEvent event) {
        String userId = event.getSource().getUserId();
        return reply("登録ありがとうございます！私は植物の生活をより良いものにするためのサポートをします！" +
                "\n" + "利用方法は「温度」「湿度」「光」「水分」というと植物の各状況がわかり、何をしたらいいのかがわかるようになっています！" +
                "\n");
    }

    // 返答メッセージを作る
    private TextMessage reply(String text) {
        return new TextMessage(text);
    }

    // 文章で話しかけられたとき（テキストメッセージのイベント）に対応する
    @EventMapping
    public Message handleMessage(MessageEvent<TextMessageContent> event) {
        TextMessageContent tmc = event.getMessage();
        String text = tmc.getText();
        switch (text) {
            case "温度":
                return celsius_degree();
            case "湿度":
                return humidity();
            case "光":
                return analog();
            case "水分":
                return moisture();
            case "報告":
                return get();
            default:
                return reply(text);
        }

    }

    public TextMessage humidity(){
        String url = "https://us.wio.seeed.io/v1/node/GroveTempHumD0/humidity?access_token=";
        String key = "980ae5490967df92cb98e4c9087fa917";
        URI uri = URI.create(url + key);
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        try{
            Humidity humidity = restTemplate.getForObject(uri, Humidity.class);
            return reply("湿度は" + humidity.getHumidity() + "です。");
        }catch (HttpClientErrorException e){
            e.printStackTrace();
            return reply("センサーに接続できていません。確認してみてください。");
        }
    }

    public TextMessage celsius_degree(){
        String url = "https://us.wio.seeed.io/v1/node/GroveTempHumD0/temperature?access_token=";
        String key = "980ae5490967df92cb98e4c9087fa917";
        URI uri = URI.create(url + key);
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        try{
            Celsius_degree celsius_degree = restTemplate.getForObject(uri, Celsius_degree.class);
            return reply("温度は" + celsius_degree.getCelsius_degree() +
                    "です。");
        }catch (HttpClientErrorException e){
            e.printStackTrace();
            return reply("センサーに接続できていません。確認してみてください。");
        }
    }

    public TextMessage analog(){
        String key = "736862004b81b1abeed5c716cacbf048";
        String url = "https://us.wio.seeed.io/v1/node/GenericAInA0/analog?access_token=";
        URI uri = URI.create(url + key);
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        try{
            Analog analog =restTemplate.getForObject(uri, Analog.class);
            return reply("光の量は" +
                    analog.getAnalog() +
                    "です。");
        }catch (HttpClientErrorException e){
            e.printStackTrace();
            return reply("センサーに接続できていません。確認してみてください。");
        }
    }

    public TextMessage moisture() {
        String key = "a6d12fb410d75d342036d1b192f76afe";
        String url = "https://us.wio.seeed.io/v1/node/GroveMoistureA0/moisture?access_token=";
        URI uri = URI.create(url + key);
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        try {
            Moisture moisture = restTemplate.getForObject(uri, Moisture.class);
            return reply("土の湿り気は" +
                    moisture.getMoisture() +
                    "です。");
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            return reply("センサーに接続できていません。確認してみてください。");
        }
    }

  /*  @EventMapping
    public Message handlePostBack(PostbackEvent event) {
        String actionLabel = event.getPostbackContent().getData();
        switch (actionLabel) {
            case "CY":
                return reply("イイね！");
            case "CN":
                return reply("つらたん");
            case "DT":
                return reply(event.getPostbackContent().getParams().toString());
            default:
                return reply("?");
        }
    }*/

    @EventMapping
    public Message handleImg(MessageEvent<ImageMessageContent> event) {
        String msgId = event.getMessage().getId();
        Optional<String> opt = Optional.empty();
        try {
            MessageContentResponse resp = client.getMessageContent(msgId).get();
            log.info("get content{}:", resp);
            opt = makeTmpFile(resp, ".jpg");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        String path = opt.orElseGet(() -> "ファイル書き込みNG");
        return reply(path);
    }

    private Optional<String> makeTmpFile(MessageContentResponse resp, String extension) {
        try (InputStream is = resp.getStream()) {
            Path tmpFilePath = Files.createTempFile("linebot", extension);
            Files.copy(is, tmpFilePath, StandardCopyOption.REPLACE_EXISTING);
            return Optional.ofNullable(tmpFilePath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    //quickreplayの機能
//        @GetMapping("quickreplay")
    public Message get() {
        final List<QuickReplyItem> items = Arrays.<QuickReplyItem>asList(
                QuickReplyItem.builder()
                        .action(new MessageAction("gishi-yama", "https://github.com/gishi-yama/linebot-java-handson"))
                        .build(),
                QuickReplyItem.builder()
                        .action(CameraAction.withLabel("植物の写真を撮ろう！"))
                        .build(),
                QuickReplyItem.builder()
                        .action(CameraRollAction.withLabel("撮った写真を確認！"))
                        .build(),
                QuickReplyItem.builder()
                        .action(PostbackAction.builder()
                                .label("PostbackAction")
                                .text("PostbackAction clicked")
                                .data("{PostbackAction: true}")
                                .build())
                        .build()
        );

        final QuickReply quickReply = QuickReply.items(items);

        return TextMessage
                .builder()
                .text("植物に優しい環境を！！")
                .quickReply(quickReply)
                .build();
    }




}