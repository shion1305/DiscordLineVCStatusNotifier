package com.shion1305.discord.lineNotifier;

public class MessageData {
    long userID;
    long messageID;
    long channelID;

    public MessageData(long userID, long channelID, long messageID) {
        this.userID = userID;
        this.messageID = messageID;
        this.channelID = channelID;
    }
}
