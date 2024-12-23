package hoogas_client;

import java.util.Map;

public interface HoogasMessageListener {


    void onPublicConfigUpdate(Map<String,String> publicConfig);

    void onStop();
}
