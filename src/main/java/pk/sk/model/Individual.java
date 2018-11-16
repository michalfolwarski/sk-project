package pk.sk.model;

public class Individual {
    private int group;
    private IndividualType type;
    private int position;

    public Individual(int group, int position) {
        this(group, position, IndividualType.COOPERATOR);
    }

    public Individual(int group, int position, IndividualType type) {
        this.group = group;
        this.type = type;
        this.position = position;
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

    @Override
    public String toString() {
        return "Individual{" +
                "group=" + group +
                ", type=" + type +
                ", position=" + position +
                '}';
    }
}

