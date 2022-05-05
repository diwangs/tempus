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
    private Set<List<String>> paths = new HashSet<List<String>>();
    private Long threshold;
    private Double confidenceLevel;
    
    public Config(String filePath) {
        try {
            JSONObject jo = (JSONObject) new JSONParser().parse(new FileReader(filePath));
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
            this.threshold = (Long) intent.get("threshold");

            // Read possible paths
            JSONArray pathsJSON = (JSONArray) intent.get("paths");
            Iterator psitr = pathsJSON.iterator();
            while (psitr.hasNext()) {
                JSONArray pathJSON = (JSONArray) psitr.next();
                Iterator<String> pitr = pathJSON.iterator();
                List<String> path = new LinkedList<String>();
                path.add("Tx");
                while(pitr.hasNext()) {
                    path.add(pitr.next());
                }
                path.add("Rx");
                this.paths.add(path);
            }

            // Read "hyperparameter"
            this.confidenceLevel = (Double) jo.get("confidenceLevel");
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

    public Set<List<String>> getPaths() {
        return paths;
    }

    public Long getThreshold() {
        return threshold;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }
}
