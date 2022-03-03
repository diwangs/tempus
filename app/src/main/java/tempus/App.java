/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package tempus;

import java.util.*;
import java.net.URL;

import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.system.*;
import com.uppaal.engine.Engine;
import com.uppaal.engine.Problem;
import com.uppaal.engine.QueryFeedback;
import com.uppaal.engine.QueryResult;
import com.uppaal.model.core2.Query;
import com.uppaal.model.system.symbolic.SymbolicTrace;
import com.uppaal.model.system.concrete.ConcreteTrace;

public class App {

    static SymbolicTrace strace = null;
	static ConcreteTrace ctrace = null;
    public static final String options = "--search-order 0 --diagnostic 0";

    public static void main(String[] args) {
        try {
            com.uppaal.model.io2.XMLReader.setXMLResolver(new com.uppaal.model.io2.UXMLResolver());

            // Model contruction (reading XML for now)
            Document doc = new PrototypeDocument().load(new URL("file:///home/diwangs/Codes/PhD/tempus/models/packet-based.xml"));
			Link.createSampleLink(doc);

			// System declaration
			doc.setProperty("system", 
				"L_1_20 = Link(10, 1, 20, 99, 1);" +
				"L_2_20 = Link(11, 2, 20, 99, 1);" +
				"L_3_21 = Link(12, 3, 21, 99, 1);" +
				"L_4_21 = Link(13, 4, 21, 99, 1);" +
				"L_20_21 = Link(14, 21, 20, 99, 1);" +
				
				"H1 = PingHost(1);" +
				"H2 = PongHost(4);" + // Ack
				
				"S20 = Switch(20);" +
				"S21 = Switch(21);" +
				
				"P1 = Packet(1, 1, 4);" + // Ping
				"P2 = Packet(2, 4, 1);" + // Pong
				
				"system H1, H2, P1, P2, S20, S21, L_1_20, L_2_20, L_3_21, L_4_21, L_20_21;"
			);

			doc.save("test.xml");
			
			// System.out.println(doc.getTemplate("Link").getProperties());
            
            // Engine
            Engine engine = new Engine();
            engine.setServerPath("/home/diwangs/Codes/PhD/uppaal64-4.1.25-5/bin-Linux/server");
            engine.connect();
            
            // Compilation
            ArrayList<Problem> problems = new ArrayList<Problem>();
            UppaalSystem sys = engine.getSystem(doc, problems);

            // Statistical model-checking:
			Query smcq = new Query("Pr[# <= 40](<> P1.on_receiver)", "what is the probability of finishing?");
            System.out.println("===== SMC check: " + smcq.getFormula() + " =====");
			QueryResult res = engine.query(sys, options, smcq, qf);
			System.out.println("Result: " + res);
			// To get trace? -> ctrace from sys
			// To get plot -> res.getData());

            // Disconnect, otherwise gradle won't exit
			engine.disconnect();
            
        } catch (Exception e) {
            // e.printStackTrace();
        }
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
