package pk.sk.controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import pk.sk.model.Individual;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {
    private static final int WIDTH = 100;
    private static final int HEIGHT = 100;

    private static final int DEFAULT_PIXEL = 0x00000000; // transparent black
    private static final int COOPERATORS_PIXEL = 0xFFFFFFFF; // white
    private static final int DEFECTORS_PIXEL = 0xFF000000; // black

    private static boolean isRunning = false;

    public TextField initialPopulationField;
    public TextField defectorsField;
    public TextField maxPopulationPerGroup;
    public TextField maxNumberOfGroupsField;
    public Button runButton;
    public TextField delayField;

    @FXML
    private ImageView outputContainer;
    private BufferedImage outputImage;
    private int[] originalPixels = new int[]{};
    private List<Optional<Individual>> individuals = new ArrayList<>();
    private int[] pixels = new int[]{};
    private int width;
    private int height;
    private Random random = new Random();
    private HashMap<Integer, Integer> colorsOfGroup = new HashMap<>();

    public void createOutputImage() {
        outputImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

        width = outputImage.getWidth();
        height = outputImage.getHeight();
        pixels = outputImage.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);

        originalPixels = pixels.clone();

        refreshImage();
    }

    private void cleanUp() {
        individuals.clear();
        individuals.addAll(Collections.nCopies(HEIGHT * WIDTH, Optional.empty()));
    }

    private void setupOutputContainer() {
        outputContainer.setPreserveRatio(true);
        outputContainer.setFitHeight(500);
        outputContainer.setSmooth(true);
    }

    private void generateRandomLeaders() {
        //todo
    }

    private void generateRandomPopulation() {
        int length = pixels.length;

        int population = Integer.parseInt(initialPopulationField.getText());
        int defectors = Integer.parseInt(defectorsField.getText());

        Random random = new Random();
        for (int i = 0; i < defectors; i++) {
            int index = random.nextInt(length);
            pixels[index] = DEFECTORS_PIXEL;
        }

        for (int i = 0; i < population - defectors; i++){
            int index = random.nextInt(length);
            if (pixels[index] != DEFAULT_PIXEL){
                i--;
            } else {
                pixels[index] = COOPERATORS_PIXEL;
            }
        }
    }

    private void refreshImage() {
        outputImage.setRGB(0, 0, width, height, pixels, 0, width);
        outputContainer.setImage(SwingFXUtils.toFXImage(outputImage, null));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupOutputContainer();
        createFieldListeners();
        cleanUp();
    }

    private void createFieldListeners() {
        initialPopulationField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        initialPopulationField.setText(filterNumber(newValue)));
        defectorsField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        defectorsField.setText(filterNumber(newValue)));
        delayField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        delayField.setText(filterNumber(newValue)));
        maxNumberOfGroupsField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        maxNumberOfGroupsField.setText(filterNumber(newValue)));
        maxPopulationPerGroup.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        maxPopulationPerGroup.setText(filterNumber(newValue)));
    }

    private String filterNumber(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                stringBuilder.append(c);
            }
        }
        long number = 0L;
        try {
            number = Long.parseLong(stringBuilder.toString());
        } catch (NumberFormatException ignored) {
        }

        if (number >= 0)
            return number + "";
        return "";
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
        generateRandomLeaders();
        generateRandomPopulation();
        refreshImage();
        startAnimation();
    }

    private void startAnimation() {
        new Thread(() -> {
            while (isRunning) {
                nextStep();
                refreshImage();
                try {
                    int sleepTime = Integer.parseInt(delayField.getText());
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void nextStep() {
        //TODO
        System.out.println("next step");

    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (initialPopulationField.getText() == null || initialPopulationField.getText().length() == 0) {
            errorMessage += "Empty field: 'Initial population'!\n";
        }
        if (defectorsField.getText() == null || defectorsField.getText().length() == 0) {
            errorMessage += "Empty field: 'Defectors'!\n";
        }
        if (maxNumberOfGroupsField.getText() == null || maxNumberOfGroupsField.getText().length() == 0) {
            errorMessage += "Empty field: 'Max number of groups'!\n";
        }
        if (maxPopulationPerGroup.getText() == null || maxPopulationPerGroup.getText().length() == 0) {
            errorMessage += "Empty field: 'Max population per group'!\n";
        }

        if (delayField.getText() == null || delayField.getText().length() == 0) {
            errorMessage += "Empty field: 'Delay [ms]'!\n";
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
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("");
            alert.setContentText(finalErrorMessage);
            alert.showAndWait();
        });
    }

    public static void quit() {
        isRunning = false;
    }
}
