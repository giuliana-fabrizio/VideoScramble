import javafx.stage.FileChooser;

import java.io.File;


/**
 * Le contrôleur de notre application AudioScramble, où se trouve la logique de l'application mis en œuvre.
 *
 * @author Giuliana Godail Fabrizio
 * @author Mathéo Girard
 */
public class AudioScrambleController {

    public static final int N = 44;
    public static final int FREQUENCY = 12800;
    private final AudioScrambleView view;
    private String selectedFilePath;


    /**
     * Constructeur pour AudioScrambleController.
     *
     * @param view {@link AudioScrambleView} : interface utilisateur de l'application.
     */
    public AudioScrambleController(AudioScrambleView view) {
        this.view = view;

        this.view.getButtonChooseFile().setOnAction(event -> openFileChooser());
        this.view.getButtonScramble().setOnAction(event -> scramble("crypted"));
        this.view.getButtonUnscramble().setOnAction(event -> scramble("decrypted"));
        this.view.getButtonPlay().setOnAction(event -> playMusic());

        selectedFilePath = "";
    }

    /**
     * Ouvre un dialogue pour choisir un fichier audio.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Fichiers WAV (*.wav)", "*.wav");

        fileChooser.setTitle("Ouvrir un fichier audio");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showOpenDialog(view);

        if (file != null) {
            selectedFilePath = file.getAbsolutePath();
            view.buttonDisable(false);
        }
    }

    /**
     * Gère la vue avant et après le traîtement audio de la variable selectedFilePath
     *
     * @param treatment {@link  String} : type de traitement ("crypted" ou "decrypted").
     */
    public void scramble(String treatment) {
        view.buttonDisable(true);

        treatmentAudio(selectedFilePath, StdAudio.SAMPLE_RATE, treatment);

        view.buttonDisable(false);
        view.alert("Audio_" + treatment + ".wav");
    }

    /**
     * Joue le fichier audio sélectionné.
     */
    public void playMusic() {
        if (!selectedFilePath.isEmpty()) {
            view.makeAudioSpectrum(selectedFilePath);
        }
    }


    /**
     * Calcule une série de poids basés sur une distribution gaussienne.
     *
     * @return {@link double[]} représentant les poids gaussiens.
     */
    public static double[] gaussian() {
        double[] weights = new double[N];
        double sum = 0;
        int distance;

        for (int i = 0; i < N; i++) {
            distance = i - N / 2;
            weights[i] = Math.exp((double) -(distance * distance) / 2);
            sum += weights[i];
        }

        for (int i = 0; i < N; i++) {
            weights[i] /= sum;
        }
        return weights;
    }

    /**
     * Applique un filtre passe-bas aux échantillons audio.
     *
     * @param samples {@link double[]} : échantillons audio à filtrer.
     * @param weights {@link double[]} : poids utilisés pour le filtrage.
     * @return Un tableau de doubles représentant l'échantillon audio filtré.
     */
    public static double[] filtrePasseBas(double[] samples, double[] weights) {
        int len_samples = samples.length;
        int len_weights = weights.length;
        double[] filteredSample = new double[len_samples];
        double sum;

        for (int i = 0; i < len_samples; i++) {
            sum = 0;
            for (int j = 0; j < len_weights && i - j >= 0; j++) {
                sum += samples[i - j] * weights[j];
            }
            filteredSample[i] = sum;
        }

        return filteredSample;
    }

    /**
     * Brouille les échantillons audio en modifiant leur amplitude.
     *
     * @param samples    @{link double[]} : échantillons audio.
     * @param sampleRate {@link double} : taux d'échantillonnage de l'audio.
     */
    public static void scramblesAudio(double[] samples, double sampleRate) {
        double time;
        int len_samples = samples.length;

        for (int i = 0; i < len_samples; i++) {
            time = i / sampleRate;

            // Multiplie l'element de samples a la position i par une sinusoide
            samples[i] *= Math.sin(2 * Math.PI * FREQUENCY * time);
        }
    }

    /**
     * Traite les échantillons audio en appliquant un filtrage et un brouillage ou débrouillage.
     *
     * @param filePath   {@link String} : chemin de l'audio à traiter.
     * @param sampleRate {@link double} : taux d'échantillonnage de l'audio.
     * @param treatment  {@link  String} : type de traitement ("crypted" ou "decrypted").
     */
    public static void treatmentAudio(String filePath, double sampleRate, String treatment) {
        double[] samples = StdAudio.read(filePath);
        double[] weights = gaussian();

        samples = filtrePasseBas(samples, weights);
        scramblesAudio(samples, sampleRate);

        if (treatment.equals("decrypted")) {
            samples = filtrePasseBas(samples, weights);
        }
        StdAudio.save("Audio_" + treatment + ".wav", samples);
    }
}
