package pk.sk.controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;

    private static final int PREF_WIDTH = 500;
    private static final int PREF_HEIGHT = 500;
    private static final int WHITE_PIXEL = 0xFFFFFFFF;
    private static final int BLACK_PIXEL = 0xFF000000;

    private final FileChooser fileChooser = new FileChooser();
    public TextField initialPopulationField;
    public TextField defectorsField;
    public TextField paramOneField;
    public TextField maxPopulationPerGroup;
    public TextField paramTwoField;
    public TextField maxNumberOfGroupsField;
    public Button runButton;

    @FXML
    private ImageView outputContainer;
    private BufferedImage outputImage;
    private int[] originalPixels = new int[]{};
    private int[] pixels = new int[]{};
    private int width;
    private int height;

    public void createInputImage() {
        outputImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);

        width = outputImage.getWidth();
        height = outputImage.getHeight();
        pixels = outputImage.getRGB(0, 0, width, height, null, 0, width);

        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] = WHITE_PIXEL;
        }
        originalPixels = pixels.clone();

        refreshInputImage();
    }

    private void refreshInputImage() {
        outputImage.setRGB(0, 0, width, height, pixels, 0, width);
        outputContainer.setImage(SwingFXUtils.toFXImage(outputImage, null));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        outputContainer.setPreserveRatio(true);
        outputContainer.setFitHeight(500);
        outputContainer.setSmooth(true);

        outputContainer.requestFocus();
        outputContainer.setOnMouseClicked(event -> {
            int x = (int) Math.floor(event.getX() / PREF_WIDTH * width);
            int y = (int) Math.floor(event.getY() / PREF_HEIGHT * height);
            if (MouseButton.PRIMARY.equals(event.getButton())) {
                pixelClick(x, y, WHITE_PIXEL);
            } else if (MouseButton.SECONDARY.equals(event.getButton())) {
                pixelClick(x, y, BLACK_PIXEL);
            }
            refreshInputImage();
        });

        createFieldListeners();
    }

    private void createFieldListeners() {
        initialPopulationField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        initialPopulationField.setText(filterNumber(newValue)));
        defectorsField.textProperty()
                .addListener((observableValue, oldValue, newValue) ->
                        defectorsField.setText(filterNumber(newValue)));
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


    private void pixelClick(int x, int y, int color) {
        pixels[x + y * width] = color;
    }


    public void run() {
        //TODO
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

        if (errorMessage.length() == 0) {
            return true;
        } else {
            alertError(errorMessage, "Invalid Fields");
            return false;
        }
    }

    private void alertError(String finalErrorMessage, String title) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(runButton.getScene().getWindow());
            alert.setTitle(title);
            alert.setHeaderText("");
            alert.setContentText(finalErrorMessage);
            alert.showAndWait();
        });
    }
}
