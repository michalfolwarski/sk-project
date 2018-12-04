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
    private static final int MINIMUM_GROUP_SIZE = 3;
    private static final int MAX_COST = 30;
    private static final int MIN_COST = 3;
    private static final double BENEFIT_COSTS_COEFFICIENT = 1.0;
    private static final int MAX_COLOR = 224;
    private static final int MIN_COLOR = 80;
    private static final int WATCH_DOG_TICKS = 500;

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
                percentOfInitPopulation * numberOfGroups * maxPopulationPerGroup / 100 - countAllGroups();
        generateRandomCooperators(initPopulation);

        long numberOfDefectors = getAllIndividuals().count() * percentOfDefectors / 100;
        chooseRandomDefectors(numberOfDefectors);
    }

    private void generateRandomGroupLeaders(int numberOfGroups) {
        for (int i = 0; i < numberOfGroups; i++) {
            int position = random.nextInt(WIDTH * HEIGHT);

            if (individuals.get(position).isPresent()
                    || !getNearestNeighboursIn(position, LEADERS_RANGE).isEmpty()) {
                i--;
            } else {
                Individual newIndividual = new Individual(lastGroupNumber++, position, getRandomCosts());
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
        int red = getRandomComponentOfColor();
        int green = getRandomComponentOfColor();
        int blue = getRandomComponentOfColor();

        return (blue) | (green << 8) | (red << 16) | (0XFF << 24);
    }

    private int getRandomComponentOfColor() {
        return random.nextInt(MAX_COLOR - MIN_COLOR) + MIN_COLOR;
    }

    private void generateRandomCooperators(long population) {
        int range = WIDTH * HEIGHT;
        int watchDog = WATCH_DOG_TICKS;
        for (int i = 0; i < population; i++) {
            if (watchDog-- == 0) {
                System.err.println("WatchDog can not hold it anymore! (created population: "
                        + (i + lastGroupNumber) + " of " + (population + lastGroupNumber) + ")");
                break;
            }
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
            watchDog = WATCH_DOG_TICKS;
            Individual newIndividual = new Individual(neighbours.get(0).getGroup(), position, getRandomCosts());
            individuals.set(position, Optional.of(newIndividual));
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
                randomIndividual.setCosts(0);
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
        reproduceAllGroups();
        if (getDistinctGroups().count() > maxNumberOfGroups) {
            killRandomGroup();
        }
    }

    private void selectAndTrySplitGroups() {
        getDistinctGroups()
                .filter(groupNo -> getGroupSize(groupNo) >= maxPopulationPerGroup)
                .forEach(this::tryToSplitGroup);
    }

    private Stream<Integer> getDistinctGroups() {
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
            if (getDistinctGroups().count() >= maxNumberOfGroups
                    || random.nextDouble() < chanceToKillingGroup) {
                killRandomGroupExceptFor(groupNo);
            }
            splitGroup(groupNo);
        } else {
            killRandomIndividual(groupNo);
        }
    }

    private void killRandomGroupExceptFor(Integer exceptGroup) {
        List<Integer> groupList = getDistinctGroups()
                .filter(group -> !group.equals(exceptGroup))
                .collect(Collectors.toList());
        if (groupList.size() <= 2) {
            return;
        }
        int index = random.nextInt(groupList.size());
        removeGroup(groupList.get(index));
    }

    private void killRandomGroup() {
        getDistinctGroups().findAny().ifPresent(this::removeGroup);
    }

    private void removeGroup(int groupNo) {
        getGroup(groupNo).forEach(individual -> individuals.set(individual.getPosition(), Optional.empty()));
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
                    && countNeighboursGroups(neighbours, newGroup) >= 1) {
                moveToGroup(individual.getPosition(), newGroup);
                hasSomeoneChangedPosition.set(true);
            }
        });
        return hasSomeoneChangedPosition.get();
    }

    private long countNeighboursGroups(List<Individual> neighbours, int groupNo) {
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

    private void reproduceAllGroups() {
        getDistinctGroups().forEach(groupNo -> {
            List<Integer> emptyPositionList = getEmptyPositionForNewIndividuals(groupNo);
            Collections.shuffle(emptyPositionList);

            for (Integer position : emptyPositionList) {
                if (createOffspring(groupNo, position)) {
                    break;
                }
            }
        });
    }

    private boolean createOffspring(Integer groupNo, Integer position) {
        List<Individual> neighbours = getNearestNeighboursIn(position, INDIVIDUAL_RANGE);
        if (countNeighboursGroups(neighbours) == 1) {
            long cooperators = countCooperators(groupNo);
            IndividualType type = IndividualType.DEFECTOR;
            int costs = 0;
            if (cooperators > 0 && canCooperate(groupNo)) {
                type = IndividualType.COOPERATOR;
                costs = getRandomCosts();
            }
            Individual newIndividual = new Individual(groupNo, position, costs, type);
            individuals.set(position, Optional.of(newIndividual));
            return true;
        }
        return false;
    }

    private boolean canCooperate(Integer groupNo) {
        Integer lowestCost = getLowestCost(groupNo);
        long totalCosts = lowestCost * (getGroupSize(groupNo) - 1);
        if (totalCosts == 0) {
            return true;
        }
        long benefits = getSumOfCosts(groupNo);
        double ratio = BENEFIT_COSTS_COEFFICIENT * (benefits - lowestCost) / totalCosts;
        double criticalPoint = (double) getGroupSize(groupNo) / getDistinctGroups().count() + 1;

        return ratio > criticalPoint;
    }

    private int getLowestCost(Integer groupNo) {
        return getGroup(groupNo)
                .filter(individual -> IndividualType.COOPERATOR.equals(individual.getType()))
                .mapToInt(Individual::getCosts)
                .min()
                .orElse(0);
    }

    private int getSumOfCosts(Integer groupNo) {
        return getGroup(groupNo)
                .filter(individual -> IndividualType.COOPERATOR.equals(individual.getType()))
                .mapToInt(Individual::getCosts)
                .sum();
    }

    private int getRandomCosts() {
        return random.nextInt(MAX_COST - MIN_COST) + MIN_COST;
    }

    private long countCooperators(Integer groupNo) {
        return getGroup(groupNo)
                .map(Individual::getType)
                .filter(IndividualType.COOPERATOR::equals)
                .count();
    }

    private List<Integer> getEmptyPositionForNewIndividuals(Integer groupNo) {
        return getGroup(groupNo).map(Individual::getPosition)
                .flatMap(position -> getNeighboursPosition(position, GROUP_RANGE).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public long countAllIndividuals(IndividualType type) {
        return getAllIndividuals()
                .map(Individual::getType)
                .filter(type::equals)
                .count();
    }

    public long countAllGroups() {
        return getDistinctGroups().count();
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
