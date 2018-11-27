package pk.sk.controller;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import pk.sk.model.Individual;
import pk.sk.model.IndividualType;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final int REPRODUCTION_RATIO = 3;
    private static final int MINIMUM_GROUP_SIZE = 3;

    private static boolean isRunning = false;

    @FXML
    public ScrollPane scrollPane;
    @FXML
    private Button generateButton;
    @FXML
    private Button runButton;
    @FXML
    private Label statusBar;
    @FXML
    private Spinner<Integer> initialPopulation;
    @FXML
    private Spinner<Integer> defectors;
    @FXML
    private Spinner<Integer> delay;
    @FXML
    private Spinner<Integer> maxNumberOfGroups;
    @FXML
    private Spinner<Integer> maxPopulationPerGroup;
    @FXML
    private Spinner<Double> probabilityOfSplittingGroup;
    @FXML
    private ImageView outputContainer;

    private BufferedImage outputImage;
    private List<Optional<Individual>> individuals = new ArrayList<>();
    private int[] pixels = new int[]{};
    private Random random = new Random();
    private HashMap<Integer, Integer> colorsOfGroup = new HashMap<>();
    private long cycle;
    private int lastGroupNumber;
    private DoubleProperty zoomProperty = new SimpleDoubleProperty();

    public static void quit() {
        isRunning = false;
    }

    private void cleanUp() {
        cycle = 0;
        lastGroupNumber = 0;
        individuals.clear();
        individuals.addAll(Collections.nCopies(HEIGHT * WIDTH, Optional.empty()));
        colorsOfGroup.clear();
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
        return getAllIndividuals()
                .collect(Collectors.toList());
    }

    private long countGroups() {
        return getDistinctGroup().count();
    }

    private Stream<Integer> getDistinctGroup() {
        return getAllIndividuals()
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

    private double getChanceToSplitting() {
        return probabilityOfSplittingGroup.getValue() / 100;
    }

    private double getChanceToKillingGroup() {
        return 0.5;
    }

    private void markGroupArea(int index) {
        if (!individuals.get(index).isPresent()) {
            return;
        }
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

    private boolean isValidPosition(int x, int y) {
        return (x >= 0) && (x < WIDTH) && (y >= 0) && (y < HEIGHT);
    }

    private void refreshImage() {
        updatePixelValues();
        outputImage.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
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
        outputImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        pixels = outputImage.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);

        createEventListeners();
        zoomProperty.set(450);
        generate();
    }

    private void createEventListeners() {
        zoomProperty.addListener(observable -> {
            outputContainer.setFitWidth(zoomProperty.get());
            outputContainer.setFitHeight(zoomProperty.get());
        });

        InvalidationListener invalidationListener = observable -> {
            double min = Math.min(scrollPane.getWidth(), scrollPane.getHeight());
            zoomProperty.setValue(min - 2);
        };

        scrollPane.heightProperty().addListener(invalidationListener);
        scrollPane.widthProperty().addListener(invalidationListener);

        scrollPane.addEventFilter(ScrollEvent.ANY, event -> {
            if (event.getDeltaY() > 0) {
                zoomProperty.set(zoomProperty.get() * 1.1);
            } else if (event.getDeltaY() < 0) {
                double newValue = zoomProperty.get() / 1.1;
                double min = Math.min(scrollPane.getWidth(), scrollPane.getHeight()) - 2;
                if (newValue < min) {
                    newValue = min;
                }
                zoomProperty.set(newValue);
            }
        });
    }

    public void generate() {
        System.out.println("Cleanups...");
        cleanUp();
        generateRandomGroups();
        generateRandomPopulation();
        refreshImage();
        updateStatusBar();
        runButton.setDisable(false);
    }

    public void run() {
        if (!isInputValid()) {
            return;
        }
        isRunning = !isRunning;
        prepareScene();
        if (isRunning) {
            runButton.setText("Pause");
            startSimulation();
        } else {
            runButton.setText("Run");
        }
    }

    private void prepareScene() {
        initialPopulation.setDisable(isRunning);
        defectors.setDisable(isRunning);
        generateButton.setDisable(isRunning);
    }

    private void startSimulation() {
        new Thread(() -> {
            while (isRunning) {
                cycle++;
                nextStep();
                refreshImage();
                updateStatusBar();
                try {
                    Thread.sleep(delay.getValue() + 1);
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

    private long countIndividuals(IndividualType type) {
        return getAllIndividuals()
                .map(Individual::getType)
                .filter(type::equals)
                .count();
    }

    private void nextStep() {
        selectAndTrySplitGroups();
        reproduceGroups();
    }

    private void reproduceGroups() {
        //todo add costs to individuals and change the reproduction model
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

    private Stream<Individual> getGroup(Integer groupNo) {
        return getAllIndividuals().filter(individual -> individual.getGroup() == groupNo);
    }

    private Stream<Individual> getAllIndividuals() {
        return individuals.stream()
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private void selectAndTrySplitGroups() {
        getDistinctGroup()
                .filter(groupNo -> getGroupSize(groupNo) >= getMaxPopulationPerGroup())
                .forEach(this::tryToSplitGroup);
    }

    private void tryToSplitGroup(Integer groupNo) {
        if (getGroupSize(groupNo) == 0) {
            return;
        }
        if (random.nextDouble() < getChanceToSplitting()) {
            if (getDistinctGroup().count() > 2
                    && (getDistinctGroup().count() >= getMaxGroupNumber()
                    || random.nextDouble() < getChanceToKillingGroup())) {
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
                hasSomeoneChangedPosition.set(true);
            }
        });
        return hasSomeoneChangedPosition.get();
    }

    private void moveToGroup(int position, int groupNo) {
        individuals.get(position)
                .ifPresent(individual -> individual.setGroup(groupNo));
        colorsOfGroup.put(groupNo, getRandomColor());
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

    private boolean isInputValid() {
        String errorMessage = "";
        String header = "";

        if (WIDTH * HEIGHT < getMaxGroupNumber() * getMaxPopulationPerGroup()) {
            header += "The maximum population would exceed the total number of cells ("
                    + WIDTH * HEIGHT + ")!";
            errorMessage += "Please, decrease the 'Maximum groups' or 'Maximum population per group' value.";
        }
        if (errorMessage.length() == 0) {
            return true;
        } else {
            alertError(header, errorMessage);
            return false;
        }
    }

    private void alertError(String header, String errorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(runButton.getScene().getWindow());
            alert.setTitle("Invalid Values");
            alert.setHeaderText(header);
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }
}
