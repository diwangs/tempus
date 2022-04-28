package tempus.topology;

public class Link {
    private String u;
    private String v;
    private int delayMin;
    private int delayMax;
    private int successOdds;

    public void setU(String u) {
        this.u = u;
    }

    public void setV(String v) {
        this.v = v;
    }

    public void setDelayMin(int delayMin) {
        this.delayMin = delayMin;
    }

    public void setDelayMax(int delayMax) {
        this.delayMax = delayMax;
    }

    public void setSuccessOdds(int successOdds) {
        this.successOdds = successOdds;
    }

    public String getU() {
        return u;
    }

    public String getV() {
        return v;
    }

    public int getDelayMin() {
        return delayMin;
    }

    public int getDelayMax() {
        return delayMax;
    }

    public int getSuccessOdds() {
        return successOdds;
    }

    public void printPair() {
        System.out.println(this.u + " " + this.v);
    }
}
