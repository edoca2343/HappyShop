package ci553.happyshop.utility;
import javafx.scene.media.AudioClip;


public class SoundPlayer {


    public static void play(String path) {
        AudioClip clickSound = new AudioClip(SoundPlayer.class.getResource(path).toExternalForm());
        clickSound.play();
    }
}