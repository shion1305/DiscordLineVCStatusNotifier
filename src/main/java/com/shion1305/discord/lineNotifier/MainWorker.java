package com.shion1305.discord.lineNotifier;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.component.*;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.*;
import com.linecorp.bot.model.response.BotApiResponse;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.Color;
import reactor.core.Disposable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainWorker {
    private List<Long> actives = new ArrayList<>();
    boolean colorSwitcher = true;
    MessageData sentMessage;
    private static final Logger logger = Logger.getLogger("Discord-LineStatusNotifier");
    private final List<Disposable> disposables = new ArrayList<>();

    public void stop() {
        logger.info("SHUTDOWN SEQUENCE STARTED");
        for (Disposable d : disposables) {
            d.dispose();
        }
        disposables.clear();
        logger.info("SHUTDOWN SEQUENCE ENDED SUCCESSFULLY");
    }

    public MainWorker() {
        logger.info("MainWorker Initiated");
        GatewayDiscordClient client = DiscordClient.create(ConfigManager.getConfig("DiscordToken")).gateway().setEnabledIntents(IntentSet.all()).login().block();
        disposables.add(client.on(VoiceStateUpdateEvent.class)
                .filter(voiceStateUpdateEvent -> !voiceStateUpdateEvent.getCurrent().getMember().block().isBot())
                .filter(voiceStateUpdateEvent -> voiceStateUpdateEvent.getCurrent().getGuildId().asLong() == 865206369014775808L)
                .subscribe(voiceStateUpdateEvent -> {
                            try {
                                if (voiceStateUpdateEvent.isJoinEvent()) {
                                    Member member = voiceStateUpdateEvent.getCurrent().getMember().block();
                                    actives.add(member.getId().asLong());
                                    client.getChannelById(Snowflake.of(943676510533988422L)).subscribe(
                                            channel -> {
                                                channel.getRestChannel().createMessage(EmbedCreateSpec.builder()
                                                                .color(Color.DISCORD_WHITE)
                                                                .author(member.getNickname().isPresent() ? member.getNickname().get() : member.getUsername(), null, member.getAvatarUrl())
                                                                .title(voiceStateUpdateEvent.getCurrent().getChannel().block().getName() + "に入室しました!").build().asRequest())
                                                        .subscribe();
                                            }
                                    );

                                    if (actives.size() == 1) {
                                        member.getPrivateChannel().block().createMessage(EmbedCreateSpec.builder()
                                                        .title("ボイスチャンネルにようこそ!!")
                                                        .color(Color.GREEN)
                                                        .description("実装班Lineグループに参加を通知しますか?")
                                                        .build()).withComponents(ActionRow.of(Button.primary("notify-YES", "YES"), Button.secondary("notify-NO", "NO")))
                                                .doOnSuccess(message1 -> {
                                                    sentMessage = new MessageData(member.getId().asLong(), message1.getChannelId().asLong(), message1.getId().asLong());
                                                }).block();
                                    }
                                } else if (voiceStateUpdateEvent.isLeaveEvent()) {
                                    if (sentMessage != null) {
                                        voiceStateUpdateEvent.getCurrent().getMember().subscribe(
                                                member -> {
                                                    if (member.getId().asLong() == sentMessage.userID) {
                                                        client.getMessageById(Snowflake.of(sentMessage.channelID), Snowflake.of(sentMessage.messageID)).doOnSuccess(
                                                                message1 -> {
                                                                    if (message1 != null) {
                                                                        message1.delete().block();
                                                                    }
                                                                }
                                                        ).block();
                                                        sentMessage = null;
                                                    }
                                                }
                                        );
                                    }
                                    actives.remove(voiceStateUpdateEvent.getCurrent().getMember().block().getId().asLong());
                                }
                            } catch (Exception e) {
                                logger.severe("CRITICAL ERROR OCCURRED-1");
                                e.printStackTrace();
                            }
                        }
                ));
        disposables.add(client.on(ButtonInteractionEvent.class)
                .subscribe(buttonInteractionEvent -> {
                    try {
                        Message targetMessage = buttonInteractionEvent.getMessage().get();
                        if (buttonInteractionEvent.getCustomId().equals("notify-YES")) {
                            buttonInteractionEvent.edit().withEmbeds(EmbedCreateSpec.builder().color(Color.DISCORD_WHITE).title("Lineグループ\"22技術部実装班\"に通知しました。").description("正常に動作していない時は@shion1305まで連絡お願いします。").build())
                                    .withComponents().block();
                            ConfigManager.refresh();
                            ArrayList<FlexComponent> components = new ArrayList<>();
                            components.add(Image.builder().url(URI.create(buttonInteractionEvent.getInteraction().getUser().getAvatarUrl())).build());
                            components.add(Separator.builder().color("#FFFFFF").build());
                            ArrayList<FlexComponent> textArea = new ArrayList<>();
                            textArea.add(Text.builder().text(buttonInteractionEvent.getInteraction().getUser().getUsername()).color("#FFFFFF").size(FlexFontSize.XL).align(FlexAlign.CENTER).margin(FlexMarginSize.SM).build());
                            textArea.add(Text.builder().text("がボイスチャットに入室しました").color("#FFFFFF").wrap(true).gravity(FlexGravity.CENTER).align(FlexAlign.CENTER).build());
                            components.add(Box.builder().contents(textArea).layout(FlexLayout.VERTICAL).build());
                            FlexMessage lineMessage = FlexMessage.builder()
                                    .contents(Bubble.builder()
                                            .size(Bubble.BubbleSize.KILO)
                                            .body(Box.builder()
                                                    .backgroundColor(colorSwitcher ? "#404EED" : "#23272A")
                                                    .layout(FlexLayout.HORIZONTAL)
                                                    .contents(components).build())
                                            .build())
                                    .altText("DOT ボイスチャンネル参加通知").build();
                            colorSwitcher = !colorSwitcher;
                            PushMessage pMessage = new PushMessage(ConfigManager.getConfig("LineBotTarget"), lineMessage);
                            BotApiResponse resp = LineMessagingClient.builder(ConfigManager.getConfig("LineClientToken"))
                                    .build().pushMessage(pMessage).get();
                            logger.info("LineSent:" + resp.getMessage());
                        } else if (buttonInteractionEvent.getCustomId().equals("notify-NO")) {
                            buttonInteractionEvent.deferEdit().block();
                            targetMessage.delete().block();
                        }
                        if (sentMessage != null && sentMessage.messageID == targetMessage.getId().asLong()) {
                            sentMessage = null;
                        }
                    } catch (Exception e) {
                        logger.severe("CRITICAL EXCEPTION OCCURRED-2");
                        e.printStackTrace();
                    }
                }));
    }
}
