package hoogas_client.messaging;

public interface ConnectionListener {

    void onConnectionLost(int connectionId);

    void onConnectionEstablished(int connectionId);

    void onConnectionTimedOut(int connectionId);
}
