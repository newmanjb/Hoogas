package hoogas_client.messaging;

import hoogas_client.Constants;

import java.io.IOException;
import java.io.OutputStreamWriter;

class HoogasMessageWriter {


    private final OutputStreamWriter outputStreamWriter;


    HoogasMessageWriter(OutputStreamWriter outputStreamWriter) {
        this.outputStreamWriter = outputStreamWriter;
    }


    void doSend(String message) throws IOException {
        outputStreamWriter.write(message + Constants.MSG_SEPARATOR_CHAR);
        outputStreamWriter.flush();
    }
}
