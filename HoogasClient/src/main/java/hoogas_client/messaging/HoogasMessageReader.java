package hoogas_client.messaging;

import hoogas_client.Constants;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class HoogasMessageReader {


    private String partlyReadMessage = "";
    private final InputStreamReader reader;
    private final char[] receivedMsgBuffer;

    /**
     * @param reader Used to read the character stream
     * @param chunkSize The size of the chunks to be read from the stream in characters.
     */
    HoogasMessageReader(InputStreamReader reader, int chunkSize) {
        this.reader = reader;
        this.receivedMsgBuffer = new char[chunkSize];
    }


    /**
     * Reads the latest characters from the input stream as a chunk the size of which is specified in the constructor e.g. reads 100 characters if '100'
     * was specified.
     * It will return a list of all the messages in this chunk, in the order in which they were sent, that will completely fit into this number of characters.
     * For example, if the chunk size is 8 and the message termination character is '¬' then the following input stream would result in 2 messages being returned from the first call
     * to this method, the second and third call would return an empty list, and the fourth call would return the third message:
     * ab¬cd¬efghijklmnoprstuvwxyz¬
     * If less than the specified chunk size is available to read then only the characters that are available are processed.
     * THIS METHOD IS NOT THREAD-SAFE.
     * The message {@link Constants#MSG_SEPARATOR_CHAR} character is not included in the returned messages.
     * @throws IOException If the socket can't be read e.g. it's been closed unexpectedly
     */
    List<String> getReceivedMessages() throws IOException {

        var messageList = new ArrayList<String>();
        if(reader.ready()) {
            int numCharsRead = reader.read(receivedMsgBuffer, 0, receivedMsgBuffer.length);
            var messageStringBuilder = new StringBuilder(partlyReadMessage);


            for (int i = 0; i < numCharsRead; i++) {
                char c1 = receivedMsgBuffer[i];
                if (i == numCharsRead - 1) {
                    if (c1 != Constants.MSG_SEPARATOR_CHAR) {
                        //There's no termination character, so we've got a partly read message
                        messageStringBuilder.append(c1);
                        partlyReadMessage = messageStringBuilder.toString();
                    } else {
                        partlyReadMessage = "";
                        messageList.add(messageStringBuilder.toString());
                    }
                } else if (c1 == Constants.MSG_SEPARATOR_CHAR) {
                    messageList.add(messageStringBuilder.toString());
                    messageStringBuilder = new StringBuilder();
                } else {
                    messageStringBuilder.append(c1);
                }

                receivedMsgBuffer[i] = '\u0000';
            }
        }
        return messageList;
    }
}
