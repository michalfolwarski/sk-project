package pk.sk.model;

public class Individual {
    private int group;
    private IndividualType type;

    public Individual(){
        this(IndividualType.COOPERATOR, 0);
    }

    public Individual(int group){
        this(IndividualType.COOPERATOR, group);
    }

    public Individual(IndividualType type) {
        this(type, 0);
    }

    public Individual(IndividualType type, int group) {
        this.group = group;
        this.type = type;
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

    @Override
    public String toString() {
        return "Individual{" +
                "group=" + group +
                ", type=" + type +
                '}';
    }
}

