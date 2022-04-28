package tempus;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.uppaal.model.core2.BranchPoint;
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
		com.uppaal.model.io2.XMLReader.setXMLResolver(new com.uppaal.model.io2.UXMLResolver());
		Config c = new Config("/home/diwangs/Codes/PhD/tempus/config/test.json");
        
		// Read intent and compute routing (hardcoded for now)
		// At this point, assume the necessary links exist

		// Construct UPPAAL graph based on routing
		Document doc = new Document(new PrototypeDocument());
		Template t = doc.createTemplate();
		t.setProperty("name", "Network");

		// Construct nodes
		Map<String, Location> locations = new HashMap<String, Location>();
		Map<String, Integer> lowerBound = new HashMap<String, Integer>();
		Map<String, Integer> successOdds = new HashMap<String, Integer>();

		// Construct Tx and Rx 
		Location tx = t.createLocation();
		tx.setProperty("name", "Tx");
		tx.setProperty("init", true);
		tx.setProperty("invariant", "t<=" + 0);
		t.insert(tx, null);
		locations.put("Tx", tx);
		lowerBound.put("Tx", 0);
		Location rx = t.createLocation();
		rx.setProperty("name", "Rx");
		t.insert(rx, null);
		locations.put("Rx", rx);

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
			l.setProperty("invariant", "t<=" + router.getDelayMax());
			t.insert(l, null);
			locations.put(router.getName(), l);
			lowerBound.put(router.getName(), router.getDelayMin());
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
			l.setProperty("invariant", "t<=" + link.getDelayMax());
			t.insert(l, null);
			locations.put(name, l);
			lowerBound.put(name, link.getDelayMin());
			successOdds.put(name, link.getSuccessOdds());
		}

		// Based on the necessary routers and links, construct transitions
		Location failLoc = t.createLocation();
		t.insert(failLoc, null);
		for (int i = 0; i < c.getPath().size() - 1; i++) {
			String linkName = c.getPath().get(i) + "_" + c.getPath().get(i+1);
			
			// TODO: ECMP in router to link
			Edge e1 = t.createEdge();
			e1.setSource(locations.get(c.getPath().get(i)));
			e1.setTarget(locations.get(linkName));
			e1.setProperty("guard", "t>=" + lowerBound.get(c.getPath().get(i)));
			e1.setProperty("assignment", "t=0");
			t.insert(e1, null);
			
			// Link to routers
			BranchPoint b = t.createBranchPoint();
			t.insert(b, null);
			// Edge between link and the branch point
			Edge e2 = t.createEdge();
			e2.setSource(locations.get(linkName));
			// e2.setTarget(locations.get(c.getPath().get(i+1)));
			e2.setTarget(b);
			e2.setProperty("guard", "t>=" + lowerBound.get(linkName));
			e2.setProperty("assignment", "t=0");
			t.insert(e2, null);
			// Edge between branch point and the routers
			Edge e3 = t.createEdge();
			e3.setSource(b);
			e3.setTarget(locations.get(c.getPath().get(i+1)));
			e3.setProperty("probability", successOdds.get(linkName).toString());
			t.insert(e3, null);
			// Edge between branch point and failure nodes
			Edge e4 = t.createEdge();
			e4.setSource(b);
			e4.setTarget(failLoc);
			e4.setProperty("probability", "1");
			t.insert(e4, null);
		}
		t.setProperty("declaration", "clock total, t;");
		doc.insert(t, null);
		doc.setProperty("system", "system Network;");

		try {
			doc.save("test.xml");
		} catch (Exception e) {
			e.printStackTrace();
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
