import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.util.Optional;

/**
 * Le vue de notre application  VideoScramble.
 *
 * @author Giuliana Godail Fabrizio
 * @author Mathéo Girard
 */
public class VideoScrambleView extends Stage {
    private ImageView imageViewOriginal;
    private ImageView imageViewCoded;
    private ImageView imageViewDecoded;
    private Button buttonCamera;
    private Button buttonChooseFile;
    private HBox hboxMenu;
    private HBox hBoxImage;
    private HBox hBoxButtonImage;
    private VBox vBox;
    private final int WIDTH = 1500;
    private final int HEIGHT = 750;
    private int offset = -1;
    private int step = -1;
    private Button saveOriginalButton;
    private Button saveEncodedButton;
    private Button saveDecodedButton;
    private Label labelKey;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Constructeur de la vue VideoScramble.
     */
    public VideoScrambleView() {
        init();
        addToScene();
        alert();
        display();
    }

    /**
     * Initialise les composants de l'interface utilisateur.
     */
    public void init() {
        imageViewOriginal = new ImageView();
        imageViewCoded = new ImageView();
        imageViewDecoded = new ImageView();

        buttonCamera = new Button("Démarrer la camera");
        buttonChooseFile = new Button("Choisir un fichier");

        labelKey = new Label();

        hboxMenu = new HBox(10);
        hBoxImage = new HBox();
        hBoxButtonImage = new HBox((double) WIDTH / 6);
        vBox = new VBox(10);

        saveOriginalButton = new Button("Annuler l'enregistrement");
        saveEncodedButton = new Button("Annuler l'enregistrement");
        saveDecodedButton = new Button("Annuler l'enregistrement");
    }

    /**
     * Ajoute les composants à la scène.
     */
    public void addToScene() {
        imageViewOriginal.setFitWidth((double) WIDTH / 3);
        imageViewCoded.setFitWidth((double) WIDTH / 3);
        imageViewDecoded.setFitWidth((double) WIDTH / 3);

        imageViewOriginal.setPreserveRatio(true);
        imageViewCoded.setPreserveRatio(true);
        imageViewDecoded.setPreserveRatio(true);

        hBoxImage.getChildren().addAll(imageViewOriginal, imageViewCoded, imageViewDecoded);
        hBoxButtonImage.getChildren().addAll(saveOriginalButton, saveEncodedButton, saveDecodedButton);
        hboxMenu.getChildren().addAll(buttonCamera, buttonChooseFile);

        vBox.getChildren().addAll(hboxMenu, hBoxImage, hBoxButtonImage, labelKey);

        hboxMenu.setPadding(new Insets(10));
        hboxMenu.setAlignment(Pos.CENTER);
        hBoxImage.setAlignment(Pos.CENTER);
        hBoxButtonImage.setAlignment(Pos.CENTER);

        vBox.setAlignment(Pos.CENTER);
        vBox.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        printSaveButton(false);
    }

    /**
     * Affiche une alerte pour la saisie des clés de chiffrement.
     */
    public void alert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Clés de chiffrement");
        alert.setHeaderText("Choisir soit même ses clés");

        GridPane gridPane = new GridPane();
        Label labelOffset = new Label("Offset :");
        Label labelStep = new Label("Step :");
        TextField textFieldOffset = new TextField();
        TextField textFieldStep = new TextField();

        gridPane.add(labelOffset, 0, 0);
        gridPane.add(textFieldOffset, 1, 0);
        gridPane.add(labelStep, 0, 1);
        gridPane.add(textFieldStep, 1, 1);

        gridPane.setHgap(5); // Espacement horizontal
        gridPane.setVgap(10); // Espacement vertical

        alert.getDialogPane().setContent(gridPane);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            try {
                offset = Integer.parseInt(textFieldOffset.getText());
                step = Integer.parseInt(textFieldStep.getText());

                if (offset < 0 || offset > 256 || step < 0 || step > 128) {
                    alertFailed();
                }
            } catch (Exception e) {
                alertFailed();
            }
        }
    }

    /**
     * Configure et affiche la fenêtre principale.
     */
    public void display() {
        Scene scene = new Scene(vBox, WIDTH, HEIGHT, Color.BLACK);
        scene.getStylesheets().add("style.css");
        setScene(scene);
        setTitle("Vidéo scramble");
        show();
    }

    /**
     * Affiche une alerte en cas d'échec de la saisie des clés.
     */
    public void alertFailed() {
        offset = -1;
        step = -1;
    }

    /**
     * Affiche ou masque les boutons d'enregistrement.
     *
     * @param visible @{link boolean} Indique si les boutons doivent être visibles.
     */
    public void printSaveButton(boolean visible) {
        saveOriginalButton.setVisible(visible);
        saveEncodedButton.setVisible(visible);
        saveDecodedButton.setVisible(visible);
    }

    public void buttonDisable(boolean bool) {
        buttonCamera.setDisable(bool);
        buttonChooseFile.setDisable(bool);
    }

    // Getters.
    public Button getButtonCamera() {
        return buttonCamera;
    }

    public Button getButtonChooseFile() {
        return buttonChooseFile;
    }

    public Button getSaveOriginalButton() {
        return saveOriginalButton;
    }

    public Button getSaveEncodedButton() {
        return saveEncodedButton;
    }

    public Button getSaveDecodedButton() {
        return saveDecodedButton;
    }

    public ImageView getImageViewOriginal() {
        return imageViewOriginal;
    }

    public ImageView getImageViewCoded() {
        return imageViewCoded;
    }

    public ImageView getImageViewDecoded() {
        return imageViewDecoded;
    }

    public int getOffset() {
        return offset;
    }

    public int getStep() {
        return step;
    }


    // Setter.
    public void setButtonText(String text) {
        buttonCamera.setText(text);
    }

    public void setLabelKey(String key) {
        labelKey.setText(key);
    }
}
