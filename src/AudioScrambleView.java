import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;


/**
 * Le vue de notre application AudioScramble.
 *
 * @author Giuliana Godail Fabrizio
 * @author Mathéo Girard
 */
public class AudioScrambleView extends Stage {

    private BarChart<String, Number> barChart;
    private XYChart.Series<String, Number> topSeries;
    private XYChart.Series<String, Number> bottomSeries;

    private Button buttonChooseFile;
    private Button buttonScramble;
    private Button buttonUnscramble;
    private Button buttonPlay;
    private HBox hBoxButton;
    private HBox hBoxFile;
    private VBox vBox;
    private ImageView imageViewScramble;
    private ImageView imageViewUnscramble;
    private ImageView imageViewFile;
    private ImageView imageViewPlay;
    private MediaPlayer mediaPlayer;

    /**
     * Constructeur de la vue AudioScramble.
     */
    public AudioScrambleView() {
        init();
        initializeAudioSpectrum();
        setStyle();
        addToScene();
        display();
    }

    /**
     * Initialise les composants de l'interface utilisateur.
     */
    public void init() {
        buttonChooseFile = new Button("Choisir un fichier");
        buttonScramble = new Button("Crypter");
        buttonUnscramble = new Button("Décrypter");
        buttonPlay = new Button("Jouer");

        imageViewScramble = new ImageView("assets/scramble.png");
        imageViewUnscramble = new ImageView("assets/unscramble.png");
        imageViewFile = new ImageView("assets/file.png");
        imageViewPlay = new ImageView("assets/play.png");

        buttonScramble.setGraphic(imageViewScramble);
        buttonUnscramble.setGraphic(imageViewUnscramble);
        buttonChooseFile.setGraphic(imageViewFile);
        buttonPlay.setGraphic(imageViewPlay);

        hBoxButton = new HBox();
        hBoxFile = new HBox();
        vBox = new VBox();

        buttonScramble.setDisable(true);
        buttonUnscramble.setDisable(true);
        buttonPlay.setDisable(true);
    }

    /**
     * Définit le style des composants de l'interface utilisateur.
     */
    public void setStyle() {
        imageViewScramble.setFitWidth(20);
        imageViewScramble.setFitHeight(20);
        imageViewUnscramble.setFitWidth(20);
        imageViewUnscramble.setFitHeight(20);
        imageViewFile.setFitWidth(20);
        imageViewFile.setFitHeight(20);
        imageViewPlay.setFitWidth(20);
        imageViewPlay.setFitHeight(20);

        hBoxButton.setPadding(new Insets(10));
        hBoxFile.setPadding(new Insets(10));

        hBoxButton.setAlignment(Pos.CENTER);
        hBoxFile.setAlignment(Pos.CENTER);
        hBoxButton.setSpacing(10);
        hBoxFile.setSpacing(10);

        vBox.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    /**
     * Ajoute les composants à la scène.
     */
    public void addToScene() {
        hBoxButton.getChildren().addAll(buttonScramble, buttonUnscramble);
        hBoxFile.getChildren().addAll(buttonChooseFile, buttonPlay);
        vBox.getChildren().addAll(hBoxButton, hBoxFile, barChart);
    }

    /**
     * Configure et affiche la fenêtre principale.
     */
    public void display() {
        Scene scene = new Scene(vBox, 400, 500);
        scene.getStylesheets().add("style.css");
        setScene(scene);
        setTitle("Audio scramble");
        show();
    }

    /**
     * Affiche une alerte d'information pour indiquer la fin du traitement audio.
     *
     * @param path {@link String} : chemin du fichier audio traité, affiché dans l'en-tête de l'alerte.
     */
    public void alert(String path) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Traitement audio terminé");
        alert.setHeaderText(path + " enregistré dans VideoScramble");
        alert.showAndWait();
    }

    /**
     * Initialise le spectre audio pour l'affichage graphique.
     */
    public void initializeAudioSpectrum() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setAnimated(false);

        topSeries = new XYChart.Series<>();
        bottomSeries = new XYChart.Series<>();

        barChart.getData().add(topSeries);
        barChart.getData().add(bottomSeries);
    }

    /**
     * Arrête et libère les ressources du MediaPlayer en cours.
     */
    private void stopAndDisposeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }

    /**
     * Génère et met à jour le spectre audio à partir d'un fichier audio spécifié.
     *
     * @param path {@link String} : chemin du fichier audio à analyser pour le spectre.
     */
    public void makeAudioSpectrum(String path) {
        stopAndDisposeMediaPlayer();

        Media media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setAutoPlay(true);

        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {

            topSeries.getData().clear();
            bottomSeries.getData().clear();

            for (int i = 0; i < magnitudes.length; i++) {
                topSeries.getData().add(new XYChart.Data<>(String.valueOf(i), magnitudes[i] + 60));
                bottomSeries.getData().add(new XYChart.Data<>(String.valueOf(i), -(magnitudes[i] + 60)));
            }
        });
    }

    /**
     * Cette méthode est utilisée pour contrôler l'accessibilité des boutons pendant certaines opérations.
     *
     * @param bool {@link boolean} : si vrai, les boutons seront désactivés, sinon ils seront activés.
     */
    public void buttonDisable(boolean bool) {
        buttonChooseFile.setDisable(bool);
        buttonScramble.setDisable(bool);
        buttonUnscramble.setDisable(bool);
        buttonPlay.setDisable(bool);
    }

    // Getters pour les boutons.
    public Button getButtonChooseFile() {
        return buttonChooseFile;
    }

    public Button getButtonScramble() {
        return buttonScramble;
    }

    public Button getButtonUnscramble() {
        return buttonUnscramble;
    }

    public Button getButtonPlay() {
        return buttonPlay;
    }
}
