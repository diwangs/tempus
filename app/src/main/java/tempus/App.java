package tempus;

import java.util.*;
import java.util.stream.Collectors;

import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.Edge;
import com.uppaal.model.core2.Location;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.system.*;
import com.uppaal.engine.Engine;
import com.uppaal.engine.Problem;
import com.uppaal.engine.QueryFeedback;
import com.uppaal.engine.QueryResult;
import com.uppaal.model.core2.Query;
import com.uppaal.model.core2.Template;
import com.uppaal.model.system.symbolic.SymbolicTrace;

import tempus.topology.Link;
import tempus.topology.Router;

import com.uppaal.model.system.concrete.ConcreteTrace;

public class App {

    static SymbolicTrace strace = null;
	static ConcreteTrace ctrace = null;
    public static final String options = "--search-order 0 --diagnostic 0";

    public static void main(String[] args) {
		Config c = new Config("/home/diwangs/Codes/PhD/tempus/config/test.json");
        
		// Read intent and compute routing (hardcoded for now)
		// At this point, assume the necessary links exist

		// Construct UPPAAL graph based on routing
		Document doc = new Document(new PrototypeDocument());
		Template t = doc.createTemplate();
		t.setProperty("name", "Network");

		// Construct nodes
		// TODO: construct sender and receiver?
		Map<String, Location> locations = new HashMap<String, Location>();
		// Construct necessary routers
		List<Router> routers = c.getRouters()
			.stream()
			.filter(r -> c.getPath().contains(r.getName()))
			.collect(Collectors.toList());
		Iterator<Router> ritr = routers.iterator();
		while(ritr.hasNext()) {
			Router router = ritr.next();
			Location l = t.createLocation();
			l.setProperty("name", router.getName());
			if (router.getName().equals(c.getPath().get(0))) {
				l.setProperty("init", true);
			}
			l.setProperty("invariant", "t<=" + router.getAvgQDelay());
			l.setProperty("comments", "t>=" + router.getAvgQDelay());
			t.insert(l, null);
			locations.put(router.getName(), l);
		}

		// Construct necessary links
		List<Link> links = new LinkedList<Link>();
		for (int i = 0; i < c.getPath().size() - 1; i++) {	
			final int j = i;
			List<Link> linksTemp = c.getLinks()
				.stream()
				.filter(l -> ((l.getU().equals(c.getPath().get(j)) && l.getV().equals(c.getPath().get(j+1))) || (l.getU().equals(c.getPath().get(j+1)) && l.getV().equals(c.getPath().get(j)))))
				.collect(Collectors.toList());
			links.addAll(linksTemp);
		}
		Iterator<Link> litr = links.iterator();
		int j = 0; // index to determine whether to flip U and V
		while(litr.hasNext()) {
			Link link = litr.next();
			String name;
			if (c.getPath().get(j++).equals(link.getU())) {
				name = link.getU() + "_" + link.getV();
			} else {
				name = link.getV() + "_" + link.getU();
			}
			Location l = t.createLocation();
			l.setProperty("name", name);
			l.setProperty("invariant", "t<=" + link.getDelay());
			l.setProperty("comments", "t>=" + link.getDelay());
			t.insert(l, null);
			locations.put(name, l);
		}

		// Based on the necessary routers and links, construct transitions
		for (int i = 0; i < c.getPath().size() - 1; i++) {
			String linkName = c.getPath().get(i) + "_" + c.getPath().get(i+1);
			
			Edge e1 = t.createEdge();
			e1.setSource(locations.get(c.getPath().get(i)));
			e1.setTarget(locations.get(linkName));
			e1.setProperty("guard", locations.get(c.getPath().get(i)).getProperty("comments").getValue());
			e1.setProperty("assignment", "t=0");
			t.insert(e1, null);
			
			Edge e2 = t.createEdge();
			e2.setSource(locations.get(linkName));
			e2.setTarget(locations.get(c.getPath().get(i+1)));
			e2.setProperty("guard", locations.get(linkName).getProperty("comments").getValue());
			e2.setProperty("assignment", "t=0");
			t.insert(e2, null);
		}
		t.setProperty("declaration", "clock total, t;");
		doc.insert(t, null);
		doc.setProperty("system", "system Network;");

		try {
			doc.save("test.xml");
		} catch (Exception e) {
			//TODO: handle exception
		}

		// Prepare the verifier
        Engine engine = new Engine();
        engine.setServerPath(System.getProperty("uppaalRootPath") + "/bin-Linux/server");
		ArrayList<Problem> problems = new ArrayList<Problem>();
		Query smcq = new Query("Pr[Network.total <= " + c.getThreshold() + "](<> Network." + c.getPath().get(c.getPath().size() - 1) + ")", "what is the probability of finishing?");
		
        // Run the verifier
		try {
			engine.connect();
			UppaalSystem sys = engine.getSystem(doc, problems);
			QueryResult res = engine.query(sys, options, smcq, qf);
			System.out.println("===== SMC check: " + smcq.getFormula() + " =====");
			System.out.println("Result: " + res);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			engine.disconnect();
		}

		// Statistical model-checking:
		// 	// To get trace? -> ctrace from sys
		// 	// To get plot -> res.getData());
    }

    public static QueryFeedback qf =
		new QueryFeedback() {
			@Override
			public void setProgressAvail(boolean availability)
			{
			}

			@Override
			public void setProgress(int load, long vm, long rss, long cached, long avail, long swap, long swapfree, long user, long sys, long timestamp)
			{
			}

			@Override
			public void setSystemInfo(long vmsize, long physsize, long swapsize)
			{
			}

			@Override
			public void setLength(int length)
			{
			}

			@Override
			public void setCurrent(int pos)
			{
			}

			@Override
			public void setTrace(char result, String feedback,
								 SymbolicTrace trace, QueryResult queryVerificationResult)
			{
				strace = trace;
			}

			public void setTrace(char result, String feedback,
								 ConcreteTrace trace, QueryResult queryVerificationResult)
			{
				ctrace = trace;
			}
			@Override
			public void setFeedback(String feedback)
			{
				if (feedback != null && feedback.length() > 0) {
					System.out.println("Feedback: "+feedback);
				}
			}

			@Override
			public void appendText(String s)
			{
				if (s != null && s.length() > 0) {
					System.out.println("Append: "+s);
				}
			}

			@Override
			public void setResultText(String s)
			{
				if (s != null && s.length() > 0) {
					System.out.println("Result: "+s);
				}
			}
		};
}
