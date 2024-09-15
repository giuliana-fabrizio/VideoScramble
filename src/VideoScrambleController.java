import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Le contrôleur de notre application VideoScramble, où se trouve la logique de l'application
 * mis en œuvre. Il gère le bouton de démarrage/arrêt de la caméra et le
 * flux vidéo acquis.
 *
 * @author Giuliana Godail Fabrizio
 * @author Mathéo Girard
 */
public class VideoScrambleController {
    private final VideoScrambleView view;
    private ScheduledExecutorService timer;
    private static final int cameraId = 0;
    private final VideoCapture capture = new VideoCapture(cameraId);
    private boolean cameraActive = false;
    private int offset, step;

    private VideoWriter videoWriterOriginal;
    private VideoWriter videoWriterCrypted;
    private VideoWriter videoWriterDecrypted;

    private final String PATHVIDEOCAPTURED = "Video_captured.mp4";
    private final String PATHVIDEOCRYPTED = "Video_crypted.mp4";
    private final String PATHVIDEODECRYPTED = "Video_decrypted.mp4";


    /**
     * Constructeur du contrôleur VideoScramble.
     *
     * @param view {@link VideoScrambleView}
     */
    public VideoScrambleController(VideoScrambleView view) {
        this.view = view;

        this.view.getButtonCamera().setOnAction(event -> startCamera());
        this.view.getButtonChooseFile().setOnAction(event -> openFileChooser());

        this.view.getSaveOriginalButton().setOnAction(event -> deleteFile(PATHVIDEOCAPTURED));
        this.view.getSaveEncodedButton().setOnAction(event -> deleteFile(PATHVIDEOCRYPTED));
        this.view.getSaveDecodedButton().setOnAction(event -> deleteFile(PATHVIDEODECRYPTED));
    }

    /**
     * L'action déclenchée en appuyant sur le bouton de l'interface graphique
     */
    private void startCamera() {

        if (!this.cameraActive) {
            view.printSaveButton(true);
            view.getButtonChooseFile().setDisable(true);

            this.capture.open(cameraId);

            if (this.capture.isOpened()) {
                this.cameraActive = true;

                generateKey();

                initilizeVideoWriter(capture.get(Videoio.CAP_PROP_FPS), capture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                        capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

                Runnable frameGrabber = () -> {
                    Mat frame = grabFrame();
                    Mat frameCrypted = frame.clone();
                    scrambleLines(frame, frameCrypted, "encode");

                    Mat frameDecrypted = frame.clone();
                    scrambleLines(frameCrypted, frameDecrypted, "decode");
                    updateView(frame, frameCrypted, frameDecrypted);

                    writeInVideo(frame, frameCrypted, frameDecrypted);
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                view.setButtonText("Arrêter la camera");
            } else {
                System.err.println("Impossible to open the camera connection...");
            }
        } else {
            this.cameraActive = false;
            this.stopAcquisition();

            view.getButtonChooseFile().setDisable(false);

            releaseVideoWriter();
            view.setButtonText("Démarrer la camera");
        }
    }

    /**
     * Ouvre un sélecteur de fichiers pour choisir un fichier vidéo à brouiller/débrouiller.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Fichiers Vidéo (AVI, MKV, MP4)", "*.avi", "*.mkv", "*.mp4");

        fileChooser.setTitle("Ouvrir un fichier vidéo");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(view);

        if (file != null) {
            String selectedFilePath = file.getAbsolutePath();
            view.printSaveButton(true);
            generateKey();

            Runnable videoProcessor = () -> treatmentOfVideo(selectedFilePath);

            this.timer = Executors.newSingleThreadScheduledExecutor();
            this.timer.schedule(videoProcessor, 0, TimeUnit.MILLISECONDS);

            stopAcquisition();
        }
    }

    /**
     * Obtenez une image du flux vidéo ouvert (le cas échéant)
     *
     * @return le {@link Mat} pour l'afficher
     */
    private Mat grabFrame() {
        Mat frame = new Mat();
        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
            } catch (Exception e) {
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        return frame;
    }

    /**
     * Convertir un objet Mat (OpenCV) dans l'image correspondante pour JavaFX
     *
     * @param frame le {@link Mat} représentant le cadre actuel
     * @return l'{@link Image} à afficher
     */
    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat object: " + e);
            return null;
        }
    }

    /**
     * @param original l'objet {@link Mat} en BGR ou en niveaux de gris
     * @return le {@link BufferedImage} correspondant
     */
    private static BufferedImage matToBufferedImage(Mat original) {
        BufferedImage image;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param imageView the {@link ImageView} to update
     * @param image     the {@link Image} to show
     */
    private void updateImageView(ImageView imageView, Image image) {
        Platform.runLater(() -> imageView.imageProperty().set(image));
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened()) {
            this.capture.release();
        }
    }


    /**
     * Initialise les VideoWriters pour capturer, crypter et décrypter les vidéos.
     *
     * @param fps    {@link double} : nombre d'images par seconde de la vidéo.
     * @param width  {@link double} : largeur de la vidéo.
     * @param height {@link double} : hauteur de la vidéo.
     */
    private void initilizeVideoWriter(double fps, double width, double height) {
        videoWriterOriginal = new VideoWriter(PATHVIDEOCAPTURED, VideoWriter.fourcc('X', '2', '6', '4'),
                fps, new Size(width, height), true);

        videoWriterCrypted = new VideoWriter(PATHVIDEOCRYPTED, VideoWriter.fourcc('X', '2', '6', '4'),
                fps, new Size(width, height), true);

        videoWriterDecrypted = new VideoWriter(PATHVIDEODECRYPTED, VideoWriter.fourcc('X', '2', '6', '4'),
                fps, new Size(width, height), true);

        if (!videoWriterOriginal.isOpened() || !videoWriterCrypted.isOpened() || !videoWriterDecrypted.isOpened()) {
            System.err.println("ERR : ouverture de fichier");
            System.exit(1);
        }
    }

    /**
     * Écrit les cadres (frames) dans les fichiers vidéo correspondants.
     *
     * @param frame          {@link Mat} : cadre original.
     * @param frameCrypted   {@link Mat} : cadre crypté.
     * @param frameDecrypted {@link Mat} : cadre décrypté.
     */
    private void writeInVideo(Mat frame, Mat frameCrypted, Mat frameDecrypted) {
        videoWriterOriginal.write(frame);
        videoWriterCrypted.write(frameCrypted);
        videoWriterDecrypted.write(frameDecrypted);
    }

    /**
     * Libère les ressources des VideoWriters.
     */
    private void releaseVideoWriter() {
        videoWriterOriginal.release();
        videoWriterCrypted.release();
        videoWriterDecrypted.release();
    }

    /**
     * Supprime un fichier vidéo spécifié par son chemin.
     *
     * @param path {@link String} : chemin du fichier vidéo à supprimer.
     */
    private void deleteFile(String path) {
        File videoFile = new File(path);

        if (!videoFile.exists()) {
            System.out.println("Fichier introuvable");
        } else if (!videoFile.delete()) {
            System.err.println("ERR suppression du fichier " + path);
        } else {
            System.out.println("Vidéo supprimée avec succès");
        }
    }

    /**
     * Ajuste le délai entre les cadres pour synchroniser avec le taux de rafraîchissement de la vidéo.
     *
     * @param frameTime {@link long} : temps en millisecondes à attendre entre les cadres.
     */
    private void adjustFrameDelay(long frameTime) {
        try {
            Thread.sleep(frameTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Génère une clé aléatoire pour le processus de brouillage/débrouillage.
     */
    private void generateKey() {
        offset = VideoScramble.offset;
        step = VideoScramble.step;

        if ((offset == -1 && view.getOffset() == -1) || (step == -1 && view.getStep() == -1)) {
            Random random = new Random();
            offset = random.nextInt(256); // 8 bits
            step = random.nextInt(128); // 7 bits
        } else if (view.getOffset() != -1 && view.getStep() != -1) {
            offset = view.getOffset();
            step = view.getStep();
        }

        String key = "Clé utilisée : (" + offset + ", " + step + ")";
        view.setLabelKey(key);
        writeInFile(key);
    }

    /**
     * Écrire les clés de chiffrement dans un fichier texte.
     *
     * @param key {@link String} : clées utilisées.
     */
    public void writeInFile(String key) {
        String pathFile = "key_used.txt";
        File file = new File(pathFile);

        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(pathFile);
            fos.write(key.getBytes());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calcule la position inverse pour le processus de débrouillage.
     *
     * @param currentPosition {@link int} : position actuelle dans le cadre.
     * @param start           {@link int} : début de la plage à considérer.
     * @param puissance       {@link int} : puissance de 2 représentant la taille de la plage.
     * @return {@link int} : position inversée.
     */
    private int inversePosition(int currentPosition, int start, int puissance) {
        for (int i = start; i < start + puissance; i++) {

            // Calcule une valeur basée sur l'offset actuel, le pas (step) et l'indice 'i'.
            // Le résultat est ensuite modulé par 'puissance' et ajusté par 'start'.
            // Compare ensuite le résultat avec la valeur passée en paramètre
            if (((offset + (2 * step + 1) * i) % puissance) + start == currentPosition) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Brouille ou débrouille les lignes d'une vidéo en fonction du traitement spécifié.
     *
     * @param frame       {@link Mat} : frame original.
     * @param outputFrame {@link Mat} : frame de sortie après traitement.
     * @param treatment   {@link String} : type de traitement ("encode" pour brouiller, "decode" pour débrouiller).
     */
    public void scrambleLines(Mat frame, Mat outputFrame, String treatment) {
        int end;
        int height = frame.rows();
        int newPosition;
        int puissance;
        int start = 0;

        while (height >= 2) {
            puissance = Integer.highestOneBit(height);
            end = start + puissance;

            for (int idLine = start; idLine < end; idLine += 1) {
                if (treatment.equals("encode")) {
                    newPosition = ((offset + (2 * step + 1) * idLine) % puissance) + start;
                } else {
                    newPosition = inversePosition(idLine, start, puissance);
                }

//                newPosition = (treatment.equals("encode")) ? ((offset + (2 * step + 1) * idLine) % puissance) + start : inversePosition(idLine, start, puissance);
                frame.row(idLine).copyTo(outputFrame.row(newPosition));
            }

            height -= puissance;
            start += puissance;
        }
    }

    /**
     * Met à jour la vue avec les frames (images) traitées.
     *
     * @param frame          {@link Mat} : frame originale.
     * @param frameCrypted   {@link Mat} : frame cryptée.
     * @param frameDecrypted {@link Mat} : frame décryptée.
     */
    public void updateView(Mat frame, Mat frameCrypted, Mat frameDecrypted) {
        Image imageToShow = mat2Image(frame);
        updateImageView(view.getImageViewOriginal(), imageToShow);

        imageToShow = mat2Image(frameCrypted);
        updateImageView(view.getImageViewCoded(), imageToShow);

        imageToShow = mat2Image(frameDecrypted);
        updateImageView(view.getImageViewDecoded(), imageToShow);
    }

    /**
     * Traite une vidéo en l'encodant et en la décodant, en affichant les résultats à l'écran.
     *
     * @param path {@link String} : chemin de la vidéo à traiter.
     */
    public void treatmentOfVideo(String path) {
        VideoCapture videoCapture = new VideoCapture(path);

        if (!videoCapture.isOpened()) {
            System.err.println("ERR : ouverture du fichier");
            System.exit(1);
        }

        initilizeVideoWriter(videoCapture.get(Videoio.CAP_PROP_FPS), videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT));

        view.buttonDisable(true);

        double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        // Utilisé pour ajusté la vitesse de la vidéo
        long frameTime = (long) (500 / fps);

        Mat frame = new Mat();
        Mat frameCrypted;
        Mat frameDecrypted;

        while (videoCapture.read(frame)) {
            frameCrypted = frame.clone();
            scrambleLines(frame, frameCrypted, "encode");

            frameDecrypted = frame.clone();
            scrambleLines(frameCrypted, frameDecrypted, "decode");

            updateView(frame, frameCrypted, frameDecrypted);
            writeInVideo(frame, frameCrypted, frameDecrypted);
            adjustFrameDelay(frameTime);
        }

        frame.release();
        releaseVideoWriter();
        videoCapture.release();
        view.buttonDisable(false);

        treatmentOfAudio(path);
    }

    /**
     * Traite le son d'une vidéo en l'encodant et en le décodant.
     *
     * @param path {@link String} : chemin de la vidéo à traiter.
     */
    private void treatmentOfAudio(String path) {
        String videoCaptured = extractAudio(path);
        String videoCrypted = "Audio_crypted.wav";
        String videoDecrypted = "Audio_decrypted.wav";

        AudioScrambleController.treatmentAudio(videoCaptured, StdAudio.SAMPLE_RATE, "crypted");
        AudioScrambleController.treatmentAudio(videoCrypted, StdAudio.SAMPLE_RATE, "decrypted");

        deleteFile("video_captured_with_song.mp4");
        deleteFile("video_cryted_with_song.mp4");
        deleteFile("video_decryted_with_song.mp4");

        mergeVideoWithAudio(PATHVIDEOCAPTURED, videoCaptured, "video_captured_with_song.mp4");
        mergeVideoWithAudio(PATHVIDEOCRYPTED, videoCrypted, "video_cryted_with_song.mp4");
        mergeVideoWithAudio(PATHVIDEODECRYPTED, videoDecrypted, "video_decryted_with_song.mp4");
    }

    /**
     * Exécuter une commande.
     *
     * @param cmd {@link String} : commande à exécuter.
     */
    private void executeCommande(String... cmd) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extraire l'audio d'une viédo avec FFMPEG.
     *
     * @param videoPath {@link String} : vidéo dont il faut extraire le son.
     */
    private String extractAudio(String videoPath) {
        String audioPath = "Audio_captured.wav";
        deleteFile(audioPath);
        executeCommande("ffmpeg", "-i", videoPath, "-ab", "160k", "-ac", "2", "-ar", "44100", "-vn", audioPath);
        return audioPath;
    }

    /**
     * Réunir le son et la vidéo avec FFMPEG.
     *
     * @param videoPath {@link String} : vidéo à fusionner.
     * @param audioPath {@link String} : audio à fusionner.
     * @param outputVideoPath {@link String} : vidéo de destination.
     */
    private void mergeVideoWithAudio(String videoPath, String audioPath, String outputVideoPath) {
        executeCommande("ffmpeg", "-i", videoPath, "-i", audioPath,
                "-c:v", "copy", "-c:a", "aac", "-strict", "experimental", outputVideoPath);
    }
}
