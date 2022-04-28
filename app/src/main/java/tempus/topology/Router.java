package tempus.topology;

public class Router {
    private String name;
    private int delayMin;
    private int delayMax;

    public void setName(String name) {
        this.name = name;
    }

    public void setDelayMin(int delayMin) {
        this.delayMin = delayMin;
    }

    public void setDelayMax(int delayMax) {
        this.delayMax = delayMax;
    }

    public String getName() {
        return name;
    }

    public int getDelayMin() {
        return delayMin;
    }

    public int getDelayMax() {
        return delayMax;
    }
}
