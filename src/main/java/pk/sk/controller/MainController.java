package pk.sk.controller;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import pk.sk.model.GroupSelectionSimulator;
import pk.sk.model.IndividualType;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final int DEFAULT_PIXEL = 0x00000000; // transparent black
    private static final int COOPERATORS_PIXEL = 0xFFFFFFFF; // white
    private static final int DEFECTORS_PIXEL = 0xFF000000; // black

    private static boolean isRunning = false;
    private GroupSelectionSimulator simulator = new GroupSelectionSimulator();
    private DoubleProperty zoomProperty = new SimpleDoubleProperty();
    private BufferedImage outputImage;
    private int[] pixels = new int[]{};
    private XYChart.Series<Number, Number> defectorsSeries;
    private XYChart.Series<Number, Number> cooperatorsSeries;
    private XYChart.Series<Number, Number> totalPopulationSeries;
    @FXML
    private CheckBox animatedChart;
    @FXML
    private CheckBox randomInitialNumberOfGroups;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private LineChart<Number, Number> lineChart;
    @FXML
    private HBox hbox;
    @FXML
    private Button resetButton;
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

    public static void quit() {
        isRunning = false;
    }

    private void markGroupArea(int index) {
        if (!simulator.getIndividual(index).isPresent()) {
            return;
        }
        int group = simulator.getIndividual(index).get().getGroup();
        int color = simulator.getColorsOfGroup(group);

        GroupSelectionSimulator.getNeighboursPosition(index, 1)
                .forEach(position -> {
                    if (!simulator.getIndividual(position).isPresent()) {
                        pixels[position] = color;
                    }
                });
    }

    private void refreshImage() {
        updatePixelValues();
        int width = getWidth();
        int height = getHeight();
        outputImage.setRGB(0, 0, width, height, pixels, 0, width);
        outputContainer.setImage(SwingFXUtils.toFXImage(outputImage, null));
    }

    private void updatePixelValues() {
        markIndividuals();
        markGroups();
    }

    private void markGroups() {
        int length = getWidth() * getHeight();
        for (int i = 0; i < length; i++) {
            int finalI = i;
            simulator.getIndividual(i).ifPresent(ind -> markGroupArea(finalI));
        }
    }

    private void markIndividuals() {
        int length = getWidth() * getHeight();
        for (int i = 0; i < length; i++) {
            if (simulator.getIndividual(i).isPresent()) {
                IndividualType type = simulator.getIndividual(i).get().getType();
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

    private int getHeight() {
        return GroupSelectionSimulator.getHEIGHT();
    }

    private int getWidth() {
        return GroupSelectionSimulator.getWIDTH();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int width = getWidth();
        int height = getHeight();
        outputImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        pixels = outputImage.getRGB(0, 0, width, height, null, 0, width);

        createEventListeners();
        setupLineChart();
        reset();
    }

    private void createEventListeners() {
        createZoomPropertyListener();
        createScrollListener();
        createResizeListener();
        createFieldsListener();
    }

    private void createZoomPropertyListener() {
        zoomProperty.addListener(observable -> {
            outputContainer.setFitWidth(zoomProperty.get());
            outputContainer.setFitHeight(zoomProperty.get());
        });

    }

    private void createScrollListener() {
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

    private void createResizeListener() {
        ChangeListener<Number> hBoxChangeListener = (observableValue, oldValue, newValue) -> {
            double height = hbox.getHeight();
            double width = hbox.getWidth();
            scrollPane.setPrefWidth(height);
            zoomProperty.setValue(height - 2);
            lineChart.setPrefWidth(width - height - 4);
        };
        hbox.heightProperty().addListener(hBoxChangeListener);
        hbox.widthProperty().addListener(hBoxChangeListener);
    }

    private void createFieldsListener() {
        probabilityOfSplittingGroup.valueProperty().addListener(o ->
                simulator.setChanceToSplittingGroup(probabilityOfSplittingGroup.getValue() / 100));

        maxNumberOfGroups.valueProperty().addListener(o ->
                simulator.setMaxNumberOfGroups(maxNumberOfGroups.getValue()));

        maxPopulationPerGroup.valueProperty().addListener(o ->
                simulator.setMaxPopulationPerGroup(maxPopulationPerGroup.getValue()));

        animatedChart.selectedProperty().addListener((observableValue, oldValue, newValue) ->
                lineChart.setAnimated(newValue));
    }

    public void reset() {
        System.out.println("Cleanups...");
        simulator.initNewSimulation(getInitialNumberOfGroups(),
                initialPopulation.getValue(),
                defectors.getValue());
        clearLineChartSeries();
        refreshViews();
        runButton.setDisable(false);
        lineChart.setAnimated(animatedChart.isSelected());
    }

    private void clearLineChartSeries() {
        lineChart.setAnimated(false);
        totalPopulationSeries.getData().clear();
        cooperatorsSeries.getData().clear();
        defectorsSeries.getData().clear();
    }

    private void refreshViews() {
        Platform.runLater(this::refreshImage);

        long cooperators = simulator.countAllIndividuals(IndividualType.COOPERATOR);
        long defectors = simulator.countAllIndividuals(IndividualType.DEFECTOR);
        long total = cooperators + defectors;
        long groups = simulator.countAllGroups();
        long cycle = simulator.getCycle();

        Platform.runLater(() -> refreshStatusBar(cooperators, defectors, total, groups, cycle));
        Platform.runLater(() -> refreshLineChart(cooperators, defectors, total, cycle));
    }

    private int getInitialNumberOfGroups() {
        if (randomInitialNumberOfGroups.isSelected()) {
            int min = Math.max(maxNumberOfGroups.getValue() / 2, 3);
            return min + new Random().nextInt(maxNumberOfGroups.getValue() - min + 1);
        }
        return maxNumberOfGroups.getValue();
    }

    private void setupLineChart() {
        lineChart.getData().clear();

        totalPopulationSeries = new XYChart.Series<>();
        totalPopulationSeries.setName("Population Size");
        lineChart.getData().add(totalPopulationSeries);

        defectorsSeries = new XYChart.Series<>();
        defectorsSeries.setName("No.of Defectors");
        lineChart.getData().add(defectorsSeries);

        cooperatorsSeries = new XYChart.Series<>();
        cooperatorsSeries.setName("No.of Cooperators");
        lineChart.getData().add(cooperatorsSeries);
    }

    public void run() {
        if (!isInputValid()) {
            return;
        }
        isRunning = !isRunning;
        prepareScene();
        if (isRunning) {
            runButton.setText("Stop");
            startSimulation();
        } else {
            runButton.setText("Start");
        }
    }

    private void prepareScene() {
        initialPopulation.setDisable(isRunning);
        defectors.setDisable(isRunning);
        resetButton.setDisable(isRunning);
        randomInitialNumberOfGroups.setDisable(isRunning);
    }

    private void startSimulation() {
        new Thread(() -> {
            while (isRunning) {
                simulator.nextStep();
                refreshViews();
                try {
                    Thread.sleep(delay.getValue() + 1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void refreshStatusBar(long cooperators, long defectors, long total, long groups, long cycle) {
        String statusMessage = String.format(
                " Total Population: %-6d Cooperators: %-6d Defectors: %-6d Groups: %-6d Cycle: %-6d",
                total, cooperators, defectors, groups, cycle);

        System.out.println(statusMessage);
        statusBar.setText(statusMessage);
    }

    private void refreshLineChart(long cooperators, long defectors, long total, long cycle) {
        cooperatorsSeries.getData().add(new XYChart.Data<>(cycle, cooperators));
        defectorsSeries.getData().add(new XYChart.Data<>(cycle, defectors));
        totalPopulationSeries.getData().add(new XYChart.Data<>(cycle, total));
    }

    private boolean isInputValid() {
        String errorMessage = "";
        String header = "";

        if (getWidth() * getHeight() < maxNumberOfGroups.getValue() * maxPopulationPerGroup.getValue()) {
            header += "The maximum population would exceed the maximum number of cells !";
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
