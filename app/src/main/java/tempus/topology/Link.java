package tempus.topology;

public class Link {
    private String u;
    private String v;
    private int delay;

    public void setU(String u) {
        this.u = u;
    }

    public void setV(String v) {
        this.v = v;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getU() {
        return u;
    }

    public String getV() {
        return v;
    }

    public int getDelay() {
        return delay;
    }

    public void printPair() {
        System.out.println(this.u + " " + this.v);
    }
}
