package pk.sk.model;

public class Individual {
    private int group;
    private IndividualType type;
    private int position;
    private int costs;


    public Individual(int group, int position, int costs) {
        this(group, position, costs, IndividualType.COOPERATOR);
    }

    public Individual(int group, int position, int costs, IndividualType type) {
        this.group = group;
        this.type = type;
        this.position = position;
        this.costs = costs;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public IndividualType getType() {
        return type;
    }

    public void setType(IndividualType type) {
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getCosts() {
        return costs;
    }

    public void setCosts(int costs) {
        this.costs = costs;
    }

    @Override
    public String toString() {
        return "Individual{" +
                "group=" + group +
                ", type=" + type +
                ", position=" + position +
                ", costs=" + costs +
                '}';
    }
}

