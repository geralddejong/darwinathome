package org.darwinathome.client;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

/**
 * livening things up a bit
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public enum SoundEffect {
    APPROACH,
    DESCEND,
    ASCEND,
    MOVE,
    WATER,
    ;

    private Clip clip;

    // Constructor to construct each element of the enum with its own sound file.

    SoundEffect() {
        try {
            String file = toString().toLowerCase() + ".wav";
            URL url = this.getClass().getClassLoader().getResource(file);
            if (url == null) {
                throw new Exception(file);
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    public void stop() {
        clip.stop();
    }

    static {
        values();
    }

    public static void main(String[] args) throws Exception {
        for (SoundEffect soundEffect : SoundEffect.values()) {
            soundEffect.play();
            Thread.sleep(5000);
        }
    }
}