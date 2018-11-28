package pk.sk.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupSelectionSimulator {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;
    private static final int LEADERS_RANGE = 3;
    private static final int INDIVIDUAL_RANGE = 2;
    private static final int GROUP_RANGE = 1;
    private static final int REPRODUCE_RANGE = 1;
    private static final int REPRODUCTION_RATIO = 3;
    private static final int MINIMUM_GROUP_SIZE = 3;

    private List<Optional<Individual>> individuals = new ArrayList<>();
    private Random random = new Random();
    private HashMap<Integer, Integer> colorsOfGroup = new HashMap<>();
    private long cycle;
    private int lastGroupNumber;
    private int maxNumberOfGroups = 10;
    private long maxPopulationPerGroup = 30;
    private double chanceToSplittingGroup = 0.02;

    public static List<Integer> getNeighboursPosition(int index, int range) {
        List<Integer> positionList = new ArrayList<>();
        int xCord = index % WIDTH;
        int yCord = index / HEIGHT;

        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                int y = yCord + i;
                int x = xCord + j;
                int position = y * WIDTH + x;
                if (isValidPosition(x, y) && position != index) {
                    positionList.add(position);
                }
            }
        }
        return positionList;
    }

    private static boolean isValidPosition(int x, int y) {
        return (x >= 0) && (x < WIDTH) && (y >= 0) && (y < HEIGHT);
    }

    public static int getWIDTH() {
        return WIDTH;
    }

    public static int getHEIGHT() {
        return HEIGHT;
    }

    private void cleanUp() {
        cycle = 0;
        lastGroupNumber = 0;
        individuals.clear();
        individuals.addAll(Collections.nCopies(HEIGHT * WIDTH, Optional.empty()));
        colorsOfGroup.clear();
    }

    public void initNewSimulation(int numberOfGroups, int percentOfInitPopulation, int percentOfDefectors) {
        cleanUp();
        generateRandomGroupLeaders(numberOfGroups);

        long initPopulation =
                percentOfInitPopulation * numberOfGroups * maxPopulationPerGroup / 100 - countGroups();
        long numberOfDefectors = (initPopulation + countGroups()) * percentOfDefectors / 100;

        generateRandomCooperators(initPopulation);
        chooseRandomDefectors(numberOfDefectors);
    }

    private void generateRandomGroupLeaders(int numberOfGroups) {
        for (int i = 0; i < numberOfGroups; i++) {
            int position = random.nextInt(WIDTH * HEIGHT);

            if (individuals.get(position).isPresent()
                    || !getNearestNeighboursIn(position, LEADERS_RANGE).isEmpty()) {
                i--;
            } else {
                Individual newIndividual = new Individual(lastGroupNumber++, position);
                individuals.set(position, Optional.of(newIndividual));
                colorsOfGroup.put(i, getRandomColor());
            }
        }
    }

    private List<Individual> getNearestNeighboursIn(int position, int range) {
        List<Individual> neighbours = new ArrayList<>();
        getNeighboursPosition(position, range)
                .forEach(coordinates -> individuals.get(coordinates).ifPresent(neighbours::add));
        return neighbours;
    }

    private int getRandomColor() {
        int alpha = 0xFF;
        int range = 160;
        int minimumValue = (255 - range) / 5 * 4;

        int red = random.nextInt(range) + minimumValue;
        int green = random.nextInt(range) + minimumValue;
        int blue = random.nextInt(range) + minimumValue;

        return (blue) | (green << 8) | (red << 16) | (alpha << 24);
    }

    private void generateRandomCooperators(long population) {
        int range = WIDTH * HEIGHT;
        for (int i = 0; i < population; i++) {
            int position = random.nextInt(range);
            if (individuals.get(position).isPresent()) {
                i--;
                continue;
            }
            List<Individual> neighbours = getNearestNeighboursIn(position, INDIVIDUAL_RANGE);
            long neighboursGroup = countNeighboursGroups(neighbours);
            if (neighboursGroup != 1
                    || getGroupSize(neighbours.get(0).getGroup()) >= maxPopulationPerGroup) {
                i--;
                continue;
            }
            individuals.set(position, Optional.of(new Individual(neighbours.get(0).getGroup(), position)));
        }
    }

    private long countNeighboursGroups(List<Individual> neighbours) {
        return neighbours.stream()
                .map(Individual::getGroup)
                .distinct()
                .count();
    }

    private long getGroupSize(int groupNo) {
        return getGroup(groupNo).count();
    }

    private Stream<Individual> getGroup(Integer groupNo) {
        return getAllIndividuals().filter(individual -> individual.getGroup() == groupNo);
    }

    private Stream<Individual> getAllIndividuals() {
        return individuals.stream()
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private void chooseRandomDefectors(long defectors) {
        List<Individual> individualList = getIndividualsList();
        for (int i = 0; i < defectors; i++) {
            int position = random.nextInt(individualList.size());
            Individual randomIndividual = individualList.get(position);

            if (IndividualType.COOPERATOR.equals(randomIndividual.getType())) {
                randomIndividual.setType(IndividualType.DEFECTOR);
            } else {
                i--;
            }
        }
    }

    private List<Individual> getIndividualsList() {
        return getAllIndividuals()
                .collect(Collectors.toList());
    }

    public void nextStep() {
        cycle++;
        selectAndTrySplitGroups();
        reproduceGroups();
    }

    private void selectAndTrySplitGroups() {
        getDistinctGroup()
                .filter(groupNo -> getGroupSize(groupNo) >= maxPopulationPerGroup)
                .forEach(this::tryToSplitGroup);
    }

    private Stream<Integer> getDistinctGroup() {
        return getAllIndividuals()
                .map(Individual::getGroup)
                .distinct();
    }

    private void tryToSplitGroup(Integer groupNo) {
        if (getGroupSize(groupNo) == 0) {
            return;
        }
        if (random.nextDouble() < chanceToSplittingGroup) {
            double chanceToKillingGroup = 0.5;
            if (getDistinctGroup().count() > 2
                    && (getDistinctGroup().count() >= maxNumberOfGroups
                    || random.nextDouble() < chanceToKillingGroup)) {
                killRandomGroupExceptFor(groupNo);
            }
            splitGroup(groupNo);
        } else {
            killRandomIndividual(groupNo);
        }
    }

    private void killRandomGroupExceptFor(Integer exceptGroup) {
        List<Integer> groupList = getDistinctGroup().collect(Collectors.toList());
        if (groupList.size() <= 2) {
            return;
        }
        for (; ; ) {
            int index = random.nextInt(groupList.size());
            if (groupList.get(index).equals(exceptGroup)) {
                continue;
            }
            removeGroup(groupList.get(index));
            break;
        }
    }

    private void removeGroup(int groupNo) {
        getGroup(groupNo)
                .forEach(individual -> individuals.set(individual.getPosition(), Optional.empty()));
        removeColorOfGroup(groupNo);
    }

    private void removeColorOfGroup(int groupNo) {
        if (getGroup(groupNo).count() == 0) {
            colorsOfGroup.remove(groupNo);
        }
    }

    private void splitGroup(Integer groupNo) {
        List<Individual> groupList = getGroup(groupNo).collect(Collectors.toList());
        int index1 = random.nextInt(groupList.size());
        int index2 = random.nextInt(groupList.size() - 1);
        int group1 = lastGroupNumber++;
        int group2 = lastGroupNumber++;

        for (; ; ) {
            if (index1 == index2) {
                index2++;
            }
            if (hasValidDistanceBetweenNewGroupLeaders(groupList.get(index1), groupList.get(index2))) {
                break;
            }
            index1 = random.nextInt(groupList.size());
            index2 = random.nextInt(groupList.size() - 1);
        }
        moveToGroup(groupList.get(index1).getPosition(), group1);
        moveToGroup(groupList.get(index2).getPosition(), group2);
        moveRestIndividualsToNewGroups(groupNo, group1, group2);
    }

    private boolean hasValidDistanceBetweenNewGroupLeaders(Individual first, Individual second) {
        if (getGroupSize(first.getGroup()) < 10) {
            return true;
        }
        return !getNeighboursPosition(first.getPosition(), INDIVIDUAL_RANGE).contains(second.getPosition());
    }

    private void moveToGroup(int position, int groupNo) {
        individuals.get(position)
                .ifPresent(individual -> individual.setGroup(groupNo));
        colorsOfGroup.put(groupNo, getRandomColor());
    }

    private void moveRestIndividualsToNewGroups(Integer oldGroup, int newGroup1, int newGroup2) {
        while (getGroupSize(oldGroup) > 0) {
            boolean hasResultInFirstGroup = findAndMove(oldGroup, newGroup1);
            boolean hasResultInSecondGroup = findAndMove(oldGroup, newGroup2);
            if (!hasResultInFirstGroup && !hasResultInSecondGroup) {
                break;
            }
        }
        removeGroup(oldGroup);
    }

    private boolean findAndMove(int oldGroup, int newGroup) {
        AtomicBoolean hasSomeoneChangedPosition = new AtomicBoolean(false);
        getGroup(oldGroup).forEach(individual -> {
            List<Individual> neighbours = getNearestNeighboursIn(individual.getPosition(), INDIVIDUAL_RANGE);

            if (countNeighboursGroups(neighbours) <= 2
                    && countNeighboursGroup(neighbours, newGroup) >= 1) {
                moveToGroup(individual.getPosition(), newGroup);
                hasSomeoneChangedPosition.set(true);
            }
        });
        return hasSomeoneChangedPosition.get();
    }

    private long countNeighboursGroup(List<Individual> neighbours, int groupNo) {
        return neighbours.stream()
                .filter(individual1 -> individual1.getGroup() == groupNo)
                .distinct()
                .count();
    }

    private void killRandomIndividual(int groupNo) {
        List<Individual> currentGroup = getGroup(groupNo)
                .collect(Collectors.toList());
        if (currentGroup.size() < MINIMUM_GROUP_SIZE) {
            return;
        }
        int index = random.nextInt(currentGroup.size());
        int position = currentGroup.get(index).getPosition();
        individuals.set(position, Optional.empty());
    }

    private void reproduceGroups() {
        //todo add costs to individuals and change the reproduction model
        getDistinctGroup().forEach(groupNo -> {
            List<Integer> emptyPositionList = getEmptyPositionForNewIndividuals(groupNo);
            if (emptyPositionList.isEmpty()) {
                return;
            }
            Set<Integer> checkedPositions = new HashSet<>();
            for (int i = 0; i < emptyPositionList.size(); i++) {
                if (emptyPositionList.size() == checkedPositions.size()) {
                    break;
                }
                int index = random.nextInt(emptyPositionList.size());
                checkedPositions.add(index);

                Integer position = emptyPositionList.get(index);
                List<Individual> neighbours = getNearestNeighboursIn(position, INDIVIDUAL_RANGE);
                if (countNeighboursGroups(neighbours) == 1) {
                    long cooperators = countIndividualsInGroup(groupNo, IndividualType.COOPERATOR);
                    long defectors = countIndividualsInGroup(groupNo, IndividualType.DEFECTOR);
                    int numberOfNeighbours = getNearestNeighboursIn(position, REPRODUCE_RANGE).size();

                    IndividualType type;
                    if (cooperators == 0) {
                        type = IndividualType.DEFECTOR;
                    } else if (defectors == 0) {
                        type = IndividualType.COOPERATOR;
                    } else if (numberOfNeighbours >= REPRODUCTION_RATIO) {
                        type = IndividualType.DEFECTOR;
                    } else {
                        type = IndividualType.COOPERATOR;
                    }

                    Individual newIndividual = new Individual(groupNo, position, type);
                    individuals.set(position, Optional.of(newIndividual));
                    break;
                }
            }
        });
    }

    private long countIndividualsInGroup(Integer groupNo, IndividualType type) {
        return getGroup(groupNo).filter(t -> t.getType().equals(type)).count();
    }

    private List<Integer> getEmptyPositionForNewIndividuals(Integer groupNo) {
        List<Integer> positionList = new ArrayList<>();
        getGroup(groupNo).map(Individual::getPosition)
                .forEach(position -> positionList.addAll(getNeighboursPosition(position, GROUP_RANGE)));

        return positionList.stream().distinct().collect(Collectors.toList());
    }

    public long countIndividuals(IndividualType type) {
        return getAllIndividuals()
                .map(Individual::getType)
                .filter(type::equals)
                .count();
    }

    public long countGroups() {
        return getDistinctGroup().count();
    }

    public Optional<Individual> getIndividual(int i) {
        return individuals.get(i);
    }

    public long getCycle() {
        return cycle;
    }

    public void setMaxNumberOfGroups(int maxNumberOfGroups) {
        this.maxNumberOfGroups = maxNumberOfGroups;
    }

    public void setMaxPopulationPerGroup(long maxPopulationPerGroup) {
        this.maxPopulationPerGroup = maxPopulationPerGroup;
    }

    public void setChanceToSplittingGroup(double chanceToSplittingGroup) {
        this.chanceToSplittingGroup = chanceToSplittingGroup;
    }

    public int getColorsOfGroup(int group) {
        return colorsOfGroup.get(group);
    }
}
