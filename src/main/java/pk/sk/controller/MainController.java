package pk.sk.controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.image.ImageView;
import pk.sk.model.Individual;
import pk.sk.model.IndividualType;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController implements Initializable {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    private static final int DEFAULT_PIXEL = 0x00000000; // transparent black
    private static final int COOPERATORS_PIXEL = 0xFFFFFFFF; // white
    private static final int DEFECTORS_PIXEL = 0xFF000000; // black
    private static final int LEADERS_RANGE = 3;
    private static final int INDIVIDUAL_RANGE = 2;
    private static final int GROUP_RANGE = 1;
    private static final int REPRODUCE_RANGE = 1;
    private static final int REPRODUCTION_RATIO = 5;

    private static boolean isRunning = false;

    public Button runButton;
    public Label statusBar;
    public Spinner<Integer> initialPopulation;
    public Spinner<Integer> defectors;
    public Spinner<Integer> delay;
    public Spinner<Integer> maxNumberOfGroups;
    public Spinner<Integer> maxPopulationPerGroup;
    public Spinner<Integer> probabilityOfSplittingGroup;

    @FXML
    private ImageView outputContainer;
    private BufferedImage outputImage;
    private List<Optional<Individual>> individuals = new ArrayList<>();
    private int[] pixels = new int[]{};
    private int width;
    private int height;
    private Random random = new Random();
    private HashMap<Integer, Integer> colorsOfGroup = new HashMap<>();
    private long cycle;
    private int lastGroupNumber;

    public static void quit() {
        isRunning = false;
    }

    public void createOutputImage() {
        outputImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

        width = outputImage.getWidth();
        height = outputImage.getHeight();
        pixels = outputImage.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);

        refreshImage();
    }

    private void cleanUp() {
        cycle = 0;
        lastGroupNumber = 0;
        individuals.clear();
        individuals.addAll(Collections.nCopies(HEIGHT * WIDTH, Optional.empty()));
    }

    private void setupOutputContainer() {
        outputContainer.setPreserveRatio(true);
        outputContainer.setFitHeight(500);
        outputContainer.setSmooth(true);
    }

    private void generateRandomGroups() {
        int maxNumberOfGroups = getMaxGroupNumber();
        int min = Math.max(maxNumberOfGroups / 2, 3);
        int numberOfGroups = min + random.nextInt(maxNumberOfGroups - min + 1);

        generateRandomGroupLeaders(numberOfGroups);
    }

    private void generateRandomGroupLeaders(int numberOfGroups) {
        for (int group = 0; group < numberOfGroups; group++) {
            int position = random.nextInt(WIDTH * HEIGHT);

            if (individuals.get(position).isPresent()
                    || !getNearestNeighboursIn(position, LEADERS_RANGE).isEmpty()) {
                group--;
            } else {
                Individual newIndividual = new Individual(lastGroupNumber++, position);
                individuals.set(position, Optional.of(newIndividual));
                colorsOfGroup.put(group, getRandomColor());
            }
        }
    }

    private void generateRandomPopulation() {
        int defectors = this.defectors.getValue() * getInitialPopulation() / 100;
        long population = getInitialPopulation() - countGroups();

        generateRandomCooperators(population);
        chooseRandomDefectors(defectors);
    }

    private void chooseRandomDefectors(int defectors) {
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
        return getIndividuals()
                .collect(Collectors.toList());
    }

    private long countGroups() {
        return getDistinctGroup().count();
    }

    private Stream<Integer> getDistinctGroup() {
        return getIndividuals()
                .map(Individual::getGroup)
                .distinct();
    }

    private void generateRandomCooperators(long population) {
        int length = WIDTH * HEIGHT;
        for (int i = 0; i < population; i++) {
            int position = random.nextInt(length);
            if (individuals.get(position).isPresent()) {
                i--;
                continue;
            }
            List<Individual> neighbours = getNearestNeighboursIn(position, INDIVIDUAL_RANGE);
            Set<Integer> neighboursGroup = neighbours.stream()
                    .map(Individual::getGroup)
                    .collect(Collectors.toSet());
            if (neighboursGroup.size() != 1
                    || getGroupSize(neighbours.get(0).getGroup()) >= getMaxPopulationPerGroup()) {
                i--;
                continue;
            }
            individuals.set(position, Optional.of(new Individual(neighbours.get(0).getGroup(), position)));
        }
    }

    private long getGroupSize(int groupNo) {
        return getGroup(groupNo).count();
    }

    private List<Individual> getNearestNeighboursIn(int position, int range) {
        List<Individual> neighbours = new ArrayList<>();
        getNeighboursPosition(position, range)
                .forEach(coordinates -> individuals.get(coordinates).ifPresent(neighbours::add));
        return neighbours;
    }

    private int getInitialPopulation() {
        return initialPopulation.getValue() * getMaxPopulation() / 100;
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

    private int getMaxPopulation() {
        return getMaxGroupNumber() * getMaxPopulationPerGroup();
    }

    private int getMaxGroupNumber() {
        return maxNumberOfGroups.getValue();
    }

    private int getMaxPopulationPerGroup() {
        return maxPopulationPerGroup.getValue();
    }

    private int getChanceToSplitting() {
        return probabilityOfSplittingGroup.getValue();
    }

    private double getChanceToKillingGroup() {
        //todo create a field for probability of killing group
        return 0.5;
    }

    private void markGroupArea(int index) {
        int group = individuals.get(index).get().getGroup();
        int color = colorsOfGroup.get(group);

        getNeighboursPosition(index, GROUP_RANGE)
                .forEach(position -> {
                    if (!individuals.get(position).isPresent()) {
                        pixels[position] = color;
                    }
                });
    }

    private List<Integer> getNeighboursPosition(int index, int range) {
        List<Integer> positionList = new ArrayList<>();
        int xCord = index % width;
        int yCord = index / height;

        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {

                int y = yCord + i;
                int x = xCord + j;
                int position = y * width + x;
                if (y < 0 || x < 0 || y >= height || x >= width
                        || position == index || position < 0 || position > WIDTH * HEIGHT) {
                    continue;
                }
                positionList.add(position);
            }
        }
        return positionList;
    }

    private void refreshImage() {
        updatePixelValues();
        outputImage.setRGB(0, 0, width, height, pixels, 0, width);
        outputContainer.setImage(SwingFXUtils.toFXImage(outputImage, null));
    }

    private void updatePixelValues() {
        markIndividuals();
        markGroups();
    }

    private void markGroups() {
        int length = WIDTH * HEIGHT;
        for (int i = 0; i < length; i++) {
            int finalI = i;
            individuals.get(i).ifPresent(ind -> markGroupArea(finalI));
        }
    }

    private void markIndividuals() {
        int length = WIDTH * HEIGHT;
        for (int i = 0; i < length; i++) {
            if (individuals.get(i).isPresent()) {
                IndividualType type = individuals.get(i).get().getType();
                if (IndividualType.COOPERATOR.equals(type)) {
                    pixels[i] = COOPERATORS_PIXEL;
                } else {
                    pixels[i] = DEFECTORS_PIXEL;
                }
            } else {
                pixels[i] = DEFAULT_PIXEL;
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupOutputContainer();
        cleanUp();
    }

    public void run() {
        if (!isInputValid()) {
            return;
        }
        if (isRunning) {
            runButton.setText("Reset & Run");
            isRunning = false;
        } else {
            runButton.setText("Stop");
            isRunning = true;
            launchSimulation();
        }
    }

    private void launchSimulation() {
        cleanUp();
        generateRandomGroups();
        generateRandomPopulation();
        refreshImage();
        updateStatusBar();
        startAnimation();
    }

    private void startAnimation() {
        new Thread(() -> {
            while (isRunning) {
                cycle++;
                nextStep();
                refreshImage();
                updateStatusBar();
                try {
                    Thread.sleep(delay.getValue());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void updateStatusBar() {
        long cooperators = countIndividuals(IndividualType.COOPERATOR);
        long defectors = countIndividuals(IndividualType.DEFECTOR);
        long total = cooperators + defectors;
        long groups = countGroups();

        String statusMessage = String.format(
                "Total Population: %-5d Cooperators: %-5d Defectors: %-5d Groups: %-5d Cycle: %-5d",
                total, cooperators, defectors, groups, cycle);

        System.out.println(statusMessage);
        Platform.runLater(() -> statusBar.setText(statusMessage));
    }

    private long countIndividuals(IndividualType cooperator) {
        return getIndividuals()
                .map(Individual::getType)
                .filter(type -> type.equals(cooperator))
                .count();
    }

    private void nextStep() {
        selectAndTrySplitGroups();
        reproduceGroups();
    }

    private void reproduceGroups() {
        List<Integer> groupList = getDistinctGroup().collect(Collectors.toList());
        groupList.forEach(groupNo -> {
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
                List<Individual> neighbours =
                        getNearestNeighboursIn(position, INDIVIDUAL_RANGE);
                if (neighbours.stream().map(Individual::getGroup).distinct().count() == 1) {
                    long cooperators = countIndividuals(groupNo, IndividualType.COOPERATOR);
                    long defectors = countIndividuals(groupNo, IndividualType.DEFECTOR);
                    int numberOfNeighbours = getNearestNeighboursIn(position, REPRODUCE_RANGE).size();

                    IndividualType type;
                    if (cooperators == 0) {
                        type = IndividualType.DEFECTOR;
                    } else if (defectors == 0) {
                        type = IndividualType.COOPERATOR;
                    } else if (numberOfNeighbours > REPRODUCTION_RATIO) {
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

    private long countIndividuals(Integer groupNo, IndividualType type) {
        return getGroup(groupNo).filter(t -> t.getType().equals(type)).count();
    }

    private List<Integer> getEmptyPositionForNewIndividuals(Integer groupNo) {
        List<Integer> positionList = new ArrayList<>();
        getGroup(groupNo).map(Individual::getPosition)
                .forEach(position -> positionList.addAll(getNeighboursPosition(position, GROUP_RANGE)));

        return positionList.stream().distinct().collect(Collectors.toList());
    }

    private Stream<Individual> getGroup(Integer groupNo) {
        return getIndividuals().filter(individual -> individual.getGroup() == groupNo);
    }

    private Stream<Individual> getIndividuals() {
        return individuals.stream()
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private void selectAndTrySplitGroups() {
        List<Integer> groupsToSplitting = new ArrayList<>();
        getDistinctGroup().forEach(groupNo -> {
            if (getGroupSize(groupNo) >= getMaxPopulationPerGroup()) {
                groupsToSplitting.add(groupNo);
            }
        });
        groupsToSplitting.forEach(this::tryToSplitGroup);
    }

    private void tryToSplitGroup(Integer groupNo) {
        if (getGroupSize(groupNo) == 0) {
            return;
        }
        if (random.nextInt(100) < getChanceToSplitting()) {
            if (getDistinctGroup().count() >= getMaxGroupNumber()
                    || random.nextDouble() < getChanceToKillingGroup()) {
                killRandomGroup(groupNo);
            }
            splitGroup(groupNo);
        } else {
            killRandomIndividual(groupNo);
        }
    }

    private void killRandomGroup(Integer exceptGroup) {
        List<Integer> groupList = getDistinctGroup().collect(Collectors.toList());
        for (; ; ) {
            int index = random.nextInt(groupList.size());
            if (groupList.get(index).equals(exceptGroup)) {
                continue;
            }
            getIndividualsList().stream()
                    .filter(individual -> individual.getGroup() == groupList.get(index))
                    .forEach(individual -> individuals.set(individual.getPosition(), Optional.empty()));
            break;
        }
    }

    private void splitGroup(Integer groupNo) {
        List<Individual> groupList = getGroup(groupNo).collect(Collectors.toList());
        int index1 = random.nextInt(groupList.size());
        int index2 = random.nextInt(groupList.size() - 1);
        if (index1 == index2) {
            index2++;
        }
        int group1 = lastGroupNumber++;
        int group2 = lastGroupNumber++;
        moveToGroup(groupList.get(index1).getPosition(), group1);
        moveToGroup(groupList.get(index2).getPosition(), group2);
        moveRestIndividualsToNewGroups(groupNo, group1, group2);
    }

    private void moveRestIndividualsToNewGroups(Integer oldGroup, int newGroup1, int newGroup2) {
        while (getGroupSize(oldGroup) > 0) {
            findAndMove(oldGroup, newGroup1);
            findAndMove(oldGroup, newGroup2);
        }
    }

    private void findAndMove(int oldGroup, int newGroup) {
        getGroup(oldGroup)
                .forEach(individual -> {
                    List<Individual> neighbours =
                            getNearestNeighboursIn(individual.getPosition(), INDIVIDUAL_RANGE);
                    long distinctNeighboursGroups = neighbours.stream()
                            .map(Individual::getGroup)
                            .distinct()
                            .count();
                    long numberOfNeighboursNewGroup = neighbours.stream()
                            .filter(individual1 -> individual1.getGroup() == newGroup)
                            .distinct()
                            .count();
                    if (distinctNeighboursGroups <= 2 && numberOfNeighboursNewGroup >= 1) {
                        moveToGroup(individual.getPosition(), newGroup);
                    } else {
                        individuals.set(individual.getPosition(), Optional.empty());
                    }
                });
    }

    private void moveToGroup(int position, int groupNo) {
        individuals.get(position)
                .ifPresent(individual -> individual.setGroup(groupNo));
        colorsOfGroup.put(groupNo, getRandomColor());
    }

    private void killRandomIndividual(int groupNo) {
        List<Individual> currentGroup = getIndividualsList().stream()
                .filter(individual -> individual.getGroup() == groupNo)
                .collect(Collectors.toList());
        int index = random.nextInt(currentGroup.size());
        int position = currentGroup.get(index).getPosition();
        individuals.set(position, Optional.empty());
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (WIDTH * HEIGHT < getMaxGroupNumber() * getMaxPopulationPerGroup()) {
            errorMessage += "Maximum population would exceed the number of cells (" + WIDTH * HEIGHT + ")!";
        }
        if (errorMessage.length() == 0) {
            return true;
        } else {
            alertError(errorMessage);
            return false;
        }
    }

    private void alertError(String finalErrorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(runButton.getScene().getWindow());
            alert.setTitle("Invalid Values");
            alert.setHeaderText("");
            alert.setContentText(finalErrorMessage);
            alert.showAndWait();
        });
    }
}
