package hoogas_client;

import com.noomtech.hoogas_shared.internal_messaging.MessageTypeToApplications;

record MsgFromHoogas(MessageTypeToApplications type, String text) {
}
