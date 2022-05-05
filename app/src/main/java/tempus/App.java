package tempus;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.uppaal.model.core2.AbstractLocation;
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

    public static void main(String[] args) {
		com.uppaal.model.io2.XMLReader.setXMLResolver(new com.uppaal.model.io2.UXMLResolver());
		Config c = new Config("/home/diwangs/Codes/PhD/tempus/config/test.json");
		System.out.println(c.getConfidenceLevel());
        Document doc = generateDocument(c);

		try {
			doc.save("test.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Prepare the verifier
        Engine engine = new Engine();
        engine.setServerPath(System.getProperty("uppaalRootPath") + "/bin-Linux/server");
		ArrayList<Problem> problems = new ArrayList<Problem>();
		Query smcq = new Query("Pr[Network.total <= " + c.getThreshold() + "](<> Network.Rx)", "what is the probability of finishing?");
		
        // Run the verifier
		try {
			engine.connect();
			UppaalSystem sys = engine.getSystem(doc, problems);
			String options = "--search-order 0 --diagnostic 0 -E " + (1 - c.getConfidenceLevel());
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

	public static Document generateDocument(Config c) {
		// Read intent and compute routing (hardcoded for now)
		// At this point, assume the necessary links exist

		// Construct UPPAAL graph based on routing
		Document doc = new Document(new PrototypeDocument());
		Template t = doc.createTemplate();
		t.setProperty("name", "Network");		

		// Construct Tx
		Location tx = t.createLocation();
		tx.setProperty("name", "Tx");
		tx.setProperty("init", true);
		tx.setProperty("invariant", "t<=" + 0);
		t.insert(tx, null);

		// Construct ecmp
		BranchPoint ecmp = t.createBranchPoint();
		t.insert(ecmp, null);
		Edge ecmpEdge = t.createEdge();
		ecmpEdge.setSource(tx);
		ecmpEdge.setTarget(ecmp);
		ecmpEdge.setProperty("guard", "t>=0");
		ecmpEdge.setProperty("assignment", "t=0");
		t.insert(ecmpEdge, null);

		// Construct Rx
		Location rx = t.createLocation();
		rx.setProperty("name", "Rx");
		t.insert(rx, null);

		Iterator<List<String>> psitr = c.getPaths().iterator();
		while (psitr.hasNext()) {
			List<String> path = psitr.next();

			// Construct nodes
			Map<String, AbstractLocation> locations = new HashMap<String, AbstractLocation>();
			locations.put("Tx", ecmp);
			locations.put("Rx", rx);
			Map<String, Integer> lowerBound = new HashMap<String, Integer>();
			Map<String, Integer> successOdds = new HashMap<String, Integer>();

			// Construct necessary routers
			List<Router> routers = c.getRouters()
				.stream()
				.filter(r -> path.contains(r.getName()))
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
			for (int i = 0; i < path.size() - 1; i++) {	
				final int j = i;
				List<Link> linksTemp = c.getLinks()
					.stream()
					.filter(l -> ((l.getU().equals(path.get(j)) && l.getV().equals(path.get(j+1))) || (l.getU().equals(path.get(j+1)) && l.getV().equals(path.get(j)))))
					.collect(Collectors.toList());
				links.addAll(linksTemp);
			}
			Iterator<Link> litr = links.iterator();
			int j = 0; // index to determine whether to flip U and V
			while(litr.hasNext()) {
				Link link = litr.next();
				String name;
				if (path.get(j++).equals(link.getU())) {
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
			for (int i = 0; i < path.size() - 1; i++) {
				String linkName = path.get(i) + "_" + path.get(i+1);
				
				// TODO: proper ECMP in router to link?
				Edge e1 = t.createEdge();
				e1.setSource(locations.get(path.get(i)));
				e1.setTarget(locations.get(linkName));
				if (path.get(i).equals("Tx")) {
					e1.setProperty("probability", "1");
				} else {
					e1.setProperty("guard", "t>=" + lowerBound.get(path.get(i)));
					e1.setProperty("assignment", "t=0");
				}
				t.insert(e1, null);
				
				// Link to routers
				BranchPoint b = t.createBranchPoint();
				t.insert(b, null);
				// Edge between link and the branch point
				Edge e2 = t.createEdge();
				e2.setSource(locations.get(linkName));
				e2.setTarget(b);
				e2.setProperty("guard", "t>=" + lowerBound.get(linkName));
				e2.setProperty("assignment", "t=0");
				t.insert(e2, null);
				// Edge between branch point and the routers
				Edge e3 = t.createEdge();
				e3.setSource(b);
				e3.setTarget(locations.get(path.get(i+1)));
				e3.setProperty("probability", successOdds.get(linkName).toString());
				t.insert(e3, null);
				// Edge between branch point and failure nodes
				Edge e4 = t.createEdge();
				e4.setSource(b);
				e4.setTarget(failLoc);
				e4.setProperty("probability", "1");
				t.insert(e4, null);
			}
		}

		t.setProperty("declaration", "clock total, t;");
		doc.insert(t, null);
		doc.setProperty("system", "system Network;");

		return doc;
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
