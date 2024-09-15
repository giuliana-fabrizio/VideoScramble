import javafx.application.Application;
import javafx.stage.Stage;

public class AudioScramble extends Application {

    @Override
    public void start(Stage primaryStage) {
        AudioScrambleView audioScrambleView = new AudioScrambleView();
        AudioScrambleController audioScrambleController = new AudioScrambleController(audioScrambleView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
