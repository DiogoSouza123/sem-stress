package com.semstress;

import java.io.BufferedInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

public class MusicaFundoPlayer {
    private Clip clip;
    private volatile AdvancedPlayer mp3Player;
    private volatile boolean pararMp3;
    private Thread threadMp3;

    public void tocarEmLoop(String recurso, int volumePercentual) {
        parar();
        if (recurso == null || recurso.trim().isEmpty()) {
            return;
        }
        if (recurso.toLowerCase().endsWith(".mp3")) {
            try {
                tocarMp3EmLoop(recurso);
            } catch (NoClassDefFoundError ex) {
                System.err.println("Suporte a MP3 indisponivel no classpath: " + ex.getMessage());
            }
            return;
        }
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(MusicaFundoPlayer.class.getResourceAsStream(recurso))
            );
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            ajustarVolume(volumePercentual);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | NullPointerException ex) {
            System.err.println("Nao foi possivel iniciar a musica de fundo: " + ex.getMessage());
            clip = null;
        }
    }

    private void tocarMp3EmLoop(final String recurso) {
        pararMp3 = false;
        threadMp3 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!pararMp3) {
                    try {
                        BufferedInputStream stream = new BufferedInputStream(
                                MusicaFundoPlayer.class.getResourceAsStream(recurso)
                        );
                        if (stream == null) {
                            System.err.println("Arquivo MP3 nao encontrado: " + recurso);
                            break;
                        }
                        mp3Player = new AdvancedPlayer(stream);
                        mp3Player.play();
                    } catch (JavaLayerException ex) {
                        if (!pararMp3) {
                            System.err.println("Erro ao tocar MP3 de fundo: " + ex.getMessage());
                        }
                        break;
                    } finally {
                        mp3Player = null;
                    }
                }
            }
        }, "musica-fundo-mp3");
        threadMp3.setDaemon(true);
        threadMp3.start();
    }

    public void parar() {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
            clip = null;
        }
        pararMp3 = true;
        if (mp3Player != null) {
            mp3Player.close();
            mp3Player = null;
        }
    }

    private void ajustarVolume(int volumePercentual) {
        if (clip == null) {
            return;
        }
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        int volume = Math.max(0, Math.min(100, volumePercentual));
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

        if (volume == 0) {
            gain.setValue(gain.getMinimum());
            return;
        }

        float min = gain.getMinimum();
        float max = gain.getMaximum();
        float ganho = min + (max - min) * (volume / 100.0f);
        gain.setValue(ganho);
    }
}
