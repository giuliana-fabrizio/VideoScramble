import javafx.application.Application;
import javafx.stage.Stage;

public class VideoScramble extends Application {

    public static int offset = -1;
    public static int step = -1;

    @Override
    public void start(Stage primaryStage) {
        VideoScrambleView videoScrambleView = new VideoScrambleView();
        VideoScrambleController videoScrambleController = new VideoScrambleController(videoScrambleView);
    }

    public static void main(String[] args) {
        if (args.length >= 2) {
            try {
                offset = Integer.parseInt(args[0]);
                step = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Paramètre de ligne de commande incorrect");
            }
        }

        launch(args);
    }
}
