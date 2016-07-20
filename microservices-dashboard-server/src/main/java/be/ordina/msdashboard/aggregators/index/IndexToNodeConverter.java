package be.ordina.msdashboard.aggregators.index;

import be.ordina.msdashboard.model.Node;
import org.json.JSONObject;
import rx.Observable;

public interface IndexToNodeConverter {

    Observable<Node> convert(String serviceId, String serviceUri, String source);

    Observable<Node> convert(String serviceId, String serviceUri, JSONObject index);

}
