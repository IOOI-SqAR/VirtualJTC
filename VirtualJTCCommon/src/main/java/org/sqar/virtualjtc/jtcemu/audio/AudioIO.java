/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Emulation von Audio-Funktionen
 * (Anschluss Kassettenrecorder und Lautsprecher)
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.jtcemu.base.JTCSys;
import org.sqar.virtualjtc.z8.Z8;

import javax.sound.sampled.*;
import java.io.IOException;


public abstract class AudioIO {
    protected static final String ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING =
            "Der Audiokanal funktioniert nicht und wurde deshalb geschlossen.";

    protected static final String ERROR_LINE_UNAVAILABLE =
            "Der Audiokanal kann nicht ge\u00F6ffnet werden,\n"
                    + "da er bereits durch eine andere Anwendung benutzt wird\n"
                    + "oder aus einem anderen Grund nicht mehr"
                    + " verf\u00FCgbar ist.";

    protected static final String ERROR_NO_LINE =
            "Der Audiokanal konnte nicht ge\u00F6ffnet werden.";

    protected static final int[] preferredLineFrameRates = {
            44100, 48000, 24000, 22050};

    protected AbstractAudioIOFld audioFld;
    protected Z8 z8;
    protected int cyclesPerSecond;
    protected int frameRate;
    protected boolean stopRequested;

    private static volatile DataLine cpuSyncLine = null;

    private boolean finishFired;
    private String errorText;


    protected AudioIO(AbstractAudioIOFld audioFld, Z8 z8) {
        this.audioFld = audioFld;
        this.z8 = z8;
        this.cyclesPerSecond = z8.getCyclesPerSecond();
        if (this.cyclesPerSecond < 1) {
            this.cyclesPerSecond = JTCSys.DEFAULT_Z8_CYCLES_PER_SECOND;
        }
        this.frameRate = 0;
        this.stopRequested = false;
        this.finishFired = false;
        this.errorText = null;
    }


    protected void checkFireFinished() {
        if (!this.finishFired) {
            this.audioFld.fireAudioFinished(this);
            this.finishFired = true;
        }
    }


    public static void checkOpenExclCPUSynchronLine() throws IOException {
        if (cpuSyncLine != null) {
            throw new IOException(
                    "Es ist bereits ein Audiokanal ge\u00F6ffnet.\n"
                            + "Sie m\u00FCssen zuerst dieses Audiokanal schlie\u00DFen,\n"
                            + "bevor Sie einen anderen \u00F6ffnen k\u00F6nnen.");
        }
    }


    public void closeCPUSynchronLine() {
        // leer
    }


    protected void closeDataLine(DataLine line) {
        if (line != null) {
            try {
                line.stop();
                line.flush();
                line.close();
                if (line == cpuSyncLine) {
                    cpuSyncLine = null;
                }
            } catch (Exception ex) {
            }
            dataLineStatusChanged();
        }
    }


    /*
     * Diese Methode wird im Emulations-Thread aufgerufen,
     * wenn ein Audiokanal gestartet oder gestoppt wird.
     * Wenn die Z8-Emulation gerade mit voller Geschwindigkeit laeuft
     * und es somit zu einer deutlichen Geschwindigkeitsaenderung kommt,
     * soll die Geschwindigkeitsberechnung neu initialisiert werden.
     */
    protected void dataLineStatusChanged() {
        if (this.z8.getCyclesPerSecond() < 1)
            this.z8.resetSpeed();
    }


    public String getErrorText() {
        return this.errorText;
    }


    public int getFrameRate() {
        return this.frameRate;
    }


    public static String getTimeText(int frameRate, long frameCount) {
        String text = "";
        if ((frameRate > 0) && (frameCount >= 0)) {
            int seconds = Math.round((float) frameCount / (float) frameRate);
            if ((seconds == 0) && (frameCount > 0)) {
                seconds = 1;
            }
            if (seconds >= 3600) {
                text = String.format(
                        "%d:%02d:%02d Stunden",
                        seconds / 3600,
                        (seconds / 60) % 60,
                        seconds % 60);
            } else if (seconds >= 60) {
                text = String.format(
                        "%02d:%02d Minuten",
                        (seconds / 60) % 60,
                        seconds % 60);
            } else {
                text = String.format("%1d Sekunden", seconds);
            }
        }
        return text;
    }


    public synchronized static boolean isCPUSynchronLine(DataLine line) {
        return (line != null) && (line == cpuSyncLine);
    }


    public static boolean isCPUSynchronLineOpen() {
        return cpuSyncLine != null;
    }


    protected SourceDataLine openSourceDataLine(
            AudioFormat format,
            Mixer mixer) throws IOException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = null;
        try {
            if (mixer != null) {
                if (mixer.isLineSupported(info)) {
                    line = (SourceDataLine) mixer.getLine(info);
                }
            } else {
                if (AudioSystem.isLineSupported(info)) {
                    line = (SourceDataLine) AudioSystem.getLine(info);
                }
            }
            if (line != null) {
                registerCPUSynchronLine(line);
                line.open(format);
                line.start();
                dataLineStatusChanged();
            }
        } catch (Exception ex) {
            closeDataLine(line);
            line = null;
            if (ex instanceof LineUnavailableException) {
                throw new IOException(ERROR_LINE_UNAVAILABLE);
            }
        }
        return line;
    }


    protected void registerCPUSynchronLine(DataLine line)
            throws IOException {
        checkOpenExclCPUSynchronLine();
        cpuSyncLine = line;
    }


    public void requestStop() {
        this.stopRequested = true;
    }


    protected void setAudioFormat(
            int frameRate,
            int sampleSizeInBits,
            int channels) {
        if ((frameRate > 0) && (sampleSizeInBits > 0)) {
            this.frameRate = frameRate;

            StringBuilder buf = new StringBuilder(64);
            buf.append(this.frameRate);
            buf.append(" Hz, ");
            buf.append(sampleSizeInBits);
            buf.append(" Bit");
            switch (channels) {
                case 1:
                    buf.append(" Mono");
                    break;
                case 2:
                    buf.append(" Stereo");
                    break;
                default:
                    buf.append(", ");
                    buf.append(channels);
                    buf.append(" Kan\u00E4le");
            }
            this.audioFld.fireFormatChanged(this, buf.toString());
        } else {
            setErrorText("Unbekanntes Audioformat");
            this.stopRequested = true;
        }
    }


    public void setErrorText(String text) {
        this.errorText = text;
    }
}
