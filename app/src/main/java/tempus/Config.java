package tempus;

import tempus.topology.Router;
import tempus.topology.Link;


import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

public class Config {
    private Set<Router> routers;
    private Set<Link> links;
    private List<String> path = new LinkedList<String>();
    private Long threshold;
    
    public Config(String path) {
        try {
            JSONObject jo = (JSONObject) new JSONParser().parse(new FileReader(path));
            ObjectMapper mapper = new ObjectMapper();

            // Read the routers
            this.routers = new HashSet<Router>();
            JSONArray routersJSON = (JSONArray) jo.get("routers");
            Iterator ritr = routersJSON.iterator();
            while (ritr.hasNext()) {
                Router r = mapper.readValue(ritr.next().toString(), Router.class);
                this.routers.add(r);
            }

            // Read the links
            this.links = new HashSet<Link>();
            JSONArray linksJSON = (JSONArray) jo.get("links");
            Iterator litr = linksJSON.iterator();
            while (litr.hasNext()) {
                Link l = mapper.readValue(litr.next().toString(), Link.class);
                this.links.add(l);
            }

            // Read properties, assume connectivity
            JSONObject intent = (JSONObject) jo.get("intent");
            JSONArray pathJSON = (JSONArray) intent.get("path");
            Iterator<String> pitr = pathJSON.iterator();
            while(pitr.hasNext()) {
                this.path.add(pitr.next());
            }
            this.threshold = (Long) intent.get("threshold");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Router> getRouters() {
        return routers;
    }

    public Set<Link> getLinks() {
        return links;
    }

    public List<String> getPath() {
        return path;
    }

    public Long getThreshold() {
        return threshold;
    }
}
