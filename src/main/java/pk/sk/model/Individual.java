package pk.sk.model;

public class Individual {
    private int group;
    private IndividualType type;
    private int position;
//
//    public Individual(){
//        this(0);
//    }
//
//    public Individual(int group){
//        this(IndividualType.COOPERATOR, group);
//    }
//
//    public Individual(IndividualType type, int group) {
//        this(group, 0, IndividualType.COOPERATOR);
//    }

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

    @Override
    public String toString() {
        return "Individual{" +
                "group=" + group +
                ", type=" + type +
                '}';
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

