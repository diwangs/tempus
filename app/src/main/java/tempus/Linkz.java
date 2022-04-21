package tempus;

import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.Template;
import com.uppaal.model.core2.Edge;
import com.uppaal.model.core2.Location;
import com.uppaal.model.core2.Property;

public class Linkz {

    public enum LKind {
        name, init, urgent, committed, invariant, exponentialrate, comments
    };

    public enum EKind {
        select, guard, synchronisation, assignment, comments
    };

    public static void setLabel(Location l, LKind kind, Object value, int x, int y) {
        l.setProperty(kind.name(), value);
        Property p = l.getProperty(kind.name());
        p.setProperty("x", x);
        p.setProperty("y", y);
    }

    public static Location addLocation(Template t, String name, String exprate,
									   int x, int y)
    {
        Location l = t.createLocation();
        t.insert(l, null);
        l.setProperty("x", x);
        l.setProperty("y", y);
		if (name != null)
			setLabel(l, LKind.name, name, x, y-28);
		if (exprate != null)
			setLabel(l, LKind.exponentialrate, exprate, x, y-28-12);
        return l;
    }

    public static void setLabel(Edge e, EKind kind, String value, int x, int y) {
        e.setProperty(kind.name(), value);
        Property p = e.getProperty(kind.name());
        p.setProperty("x", x);
        p.setProperty("y", y);
    }

    public static Edge addEdge(Template t, Location source, Location target,
							   String guard, String sync, String update)
    {
        Edge e = t.createEdge();
        t.insert(e, null);
        e.setSource(source);
        e.setTarget(target);
        int x = (source.getX()+target.getX())/2;
        int y = (source.getY()+target.getY())/2;
        if (guard != null) {
            setLabel(e, EKind.guard, guard, x-15, y-28);
        }
        if (sync != null) {
            setLabel(e, EKind.synchronisation, sync, x-15, y-14);
        }
        if (update != null) {
            setLabel(e, EKind.assignment, update, x-15, y);
        }
        return e;
    }

    public static Template createSampleLink(Document doc) {
        System.out.println("Creating links");
        Template t = doc.createTemplate(); 
		t.setProperty("name", "Experiment");
		t.setProperty("declaration", "int v;\n\nclock x,y,z;");
        System.out.println(t.getProperties());

		// the template has initial location:
		Location l0 = addLocation(t, "L0", "1", 0, 0);
		l0.setProperty("init", true);
		// add another location to the right:
		Location l1 = addLocation(t, "L1", null, 150, 0);
		setLabel(l1, LKind.invariant, "x<=10", l1.getX()-7, l1.getY()+10);
		// add another location below to the right:
		Location l2 = addLocation(t, "L2", null, 150, 150);
		setLabel(l2, LKind.invariant, "y<=20", l2.getX()-7, l2.getY()+10);
		// add another location below:
		Location l3 = addLocation(t, "L3", "1", 0, 150);
		// add another location below:
		Location lf = addLocation(t, "Final", null, -150, 150);
		// create an edge L0->L1 with an update
		Edge e = addEdge(t, l0, l1, "v<2", null, "v=1,\nx=0");
		e.setProperty(EKind.comments.name(), "Execute L0->L1 with v=1");

		// create some more edges:
		addEdge(t, l1, l2, "x>=5", null, "v=2,\ny=0");
		addEdge(t, l2, l3, "y>=10", null, "v=3,\nz=0");
		addEdge(t, l3, l0, null, null, "v=4");
		addEdge(t, l3, lf, null, null, "v=5");

        doc.insert(t, null);
        System.out.println("Link created");

        return t;
    }

    public static Template createSampleLink2(Document doc) {
        System.out.println("Creating links");
        Template t = doc.createTemplate(); 
		t.setProperty("name", "LinkJava");
        t.setProperty("parameter", "const ent_id_t eid, ent_id_t endpoint1, ent_id_t endpoint2, int success_odds, int fail_odds");
		t.setProperty("declaration", 
            "clock t;" +
            "pkt_id_t temp_pid;" +
            "ent_id_t temp_src_eid;"
        );

        Location l0 = addLocation(t, "idle", null, 0, 0);
        l0.setProperty("init", true);
        Location l1 = addLocation(t, "on_link", null, 10, 0);
        l1.setProperty("invariant", "t<5");
        Location l2 = addLocation(t, "success", null, 10, 0);
        l2.setProperty("invariant", "t<2");
        Location l3 = addLocation(t, "drop", null, 10, 0);
        l3.setProperty("invariant", "t<2");

        Edge e1 = addEdge(t, l0, l1, null, "appr[pid][eid]?", "temp_pid = pid, t = 0");
        e1.setProperty("select", "pid : pkt_id_t");
        // TODO: probability
        addEdge(t, l2, l0, "t >= 1", "leave[temp_pid][eid]!", "t = 0");
        addEdge(t, l3, l0, "t >= 1", null, "t = 0");

        doc.insert(t, null);
        System.out.println("Link created");

        return t;
    }

    public static Template createSamplePacket(Document doc) {
        Template t = doc.createTemplate(); 
		t.setProperty("name", "PacketJava");
        t.setProperty("parameter", "const pkt_id_t pid, ent_id_t src, ent_id_t dst");
		t.setProperty("declaration", 
            "clock t;" +
            "pkt_id_t temp_pid;" +
            "ent_id_t temp_src_eid;"
        );

        // Locations
        Location l0 = addLocation(t, "on_sender", null, 0, 0);
        l0.setProperty("init", true);
        Location l01 = addLocation(t, "out_sender", null, 0, 0);
        l01.setProperty("commited", true);
        Location l1 = addLocation(t, "on_link", null, 10, 0);
        Location l12 = addLocation(t, "out_link", null, 0, 0);
        l12.setProperty("commited", true);
        Location l2 = addLocation(t, "on_switch", null, 10, 0);
        Location l21 = addLocation(t, "out_switch", null, 0, 0);
        l21.setProperty("commited", true);
        Location l13 = addLocation(t, "out_link_receiver", null, 0, 0);
        l13.setProperty("commited", true);
        Location l3 = addLocation(t, "on_receiver", null, 10, 0);

        Edge e1 = addEdge(t, l0, l1, null, "appr[pid][eid]?", "temp_pid = pid, t = 0");
        e1.setProperty("select", "pid : pkt_id_t");

        addEdge(t, l2, l0, "t >= 1", "leave[temp_pid][eid]!", "t = 0");
        addEdge(t, l3, l0, "t >= 1", null, "t = 0");

        doc.insert(t, null);
        System.out.println("Link created");

        return t;
    }
}
