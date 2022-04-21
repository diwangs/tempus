package tempus.topology;

public class Router {
    private String name;
    private int avgQDelay;

    public void setName(String name) {
        this.name = name;
    }

    public void setAvgQDelay(int avgQDelay) {
        this.avgQDelay = avgQDelay;
    }

    // TODO: getters
    public String getName() {
        return name;
    }

    public int getAvgQDelay() {
        return avgQDelay;
    }

    public void printDelay() {
        System.out.println(this.avgQDelay);
    }
}
