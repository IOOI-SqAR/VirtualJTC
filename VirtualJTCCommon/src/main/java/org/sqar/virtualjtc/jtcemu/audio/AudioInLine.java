/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse zur Bedienung des Audio-Eingangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.z8.Z8;

import javax.sound.sampled.*;
import java.io.IOException;


public class AudioInLine extends AudioIn {
    private Mixer mixer;
    private TargetDataLine dataLine;
    private byte[] frameBuf;
    private byte[] audioBuf;
    private int audioLen;
    private int audioPos;
    private boolean lineRequested;


    public AudioInLine(
            AudioInFld audioInFld,
            Z8 z8,
            float thresholdValue,
            int frameRate,
            Mixer mixer) {
        super(audioInFld, z8, thresholdValue);
        this.mixer = mixer;
        this.dataLine = null;
        this.frameBuf = null;
        this.audioBuf = null;
        this.audioLen = 0;
        this.audioPos = 0;
        this.lineRequested = true;
        if (frameRate > 0) {
            this.frameRate = frameRate;
        }
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected void checkOpenSource() throws IOException {
        if (this.lineRequested && (this.dataLine == null)) {
            this.lineRequested = false;
            this.dataLine = openTargetDataLine();
        }
    }


    @Override
    public synchronized void closeCPUSynchronLine() {
        if ((this.dataLine != null) && isCPUSynchronLine(this.dataLine)) {
            this.lineRequested = false;
            closeDataLine(this.dataLine);
            this.dataLine = null;
            checkFireFinished();
        }
    }


    @Override
    protected void closeSource() {
        this.lineRequested = false;
        if (this.dataLine != null) {
            closeDataLine(this.dataLine);
            this.dataLine = null;
        }
    }


    @Override
    protected void currentCycles(long totalCycles, long diffCycles) {
        /*
         * Wenn Daten gelesen werden, darf das Soundsystem
         * auf keinen Fall auf die CPU-Emulation warten.
         * In diesem Fall wird die Geschwindigkeitsbremse
         * der CPU-Emulation temporaer, d.h.,
         * bis mindestens zum naechsten Soundsystemaufruf, abgeschaltet.
         */
        this.z8.setSpeedUnlimitedFor((int) (diffCycles * 8));
    }


    @Override
    protected byte[] readFrame() {
        byte[] frameBuf = this.frameBuf;
        byte[] audioBuf = this.audioBuf;
        TargetDataLine line = this.dataLine;
        if ((line != null)
                && (audioBuf != null)
                && (frameBuf != null)) {
            if (this.audioPos >= this.audioLen) {
                this.audioPos = 0;
                this.audioLen = line.read(
                        this.audioBuf,
                        0,
                        this.audioBuf.length);
            }
            if (this.audioPos + frameBuf.length <= this.audioLen) {
                System.arraycopy(
                        audioBuf,
                        this.audioPos,
                        frameBuf,
                        0,
                        frameBuf.length);
                this.audioPos += frameBuf.length;
            }
        }
        return frameBuf;
    }


    /* --- private Methoden --- */

    private TargetDataLine openTargetDataLine() throws IOException {
        TargetDataLine line = null;
        if (this.frameRate > 0) {
            line = openTargetDataLine(this.frameRate);
        } else {
            for (int i = 0; i < preferredLineFrameRates.length; i++) {
                line = openTargetDataLine(preferredLineFrameRates[i]);
                if (line != null) {
                    break;
                }
            }
        }
        if (line != null) {
            AudioFormat fmt = line.getFormat();
            this.frameBuf = new byte[fmt.getFrameSize()];
            this.dataLine = line;
            setAudioFormat(fmt);

            /*
             * externer Audiopuffer anlegen
             *
             * Damit die Implementierung des Blockens ausserhalb
             * der TargetDataLine.write-Methode funktioniert,
             * muss der externe Puffer kleiner als der interne sein.
             */
            this.audioBuf = new byte[Math.min(line.getBufferSize() / 4, 512)];
            this.audioPos = 0;
        } else {
            setErrorText(ERROR_NO_LINE);
            requestStop();
        }
        return line;
    }


    private TargetDataLine openTargetDataLine(
            int frameRate) throws IOException {
        TargetDataLine line = openTargetDataLine(frameRate, 2);
        if (line == null) {
            line = openTargetDataLine(frameRate, 1);
        }
        return line;
    }


    private TargetDataLine openTargetDataLine(
            int frameRate,
            int channels) throws IOException {
        AudioFormat fmt = new AudioFormat(
                (float) frameRate,
                8,
                channels,
                false,
                false);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
        TargetDataLine line = null;
        try {
            Mixer mixer = this.mixer;
            if (mixer != null) {
                if (mixer.isLineSupported(info)) {
                    line = (TargetDataLine) mixer.getLine(info);
                }
            } else {
                if (AudioSystem.isLineSupported(info)) {
                    line = (TargetDataLine) AudioSystem.getLine(info);
                }
            }
            if (line != null) {
                registerCPUSynchronLine(line);
                // interner Puffer bei Stereo fuer 125 ms
                line.open(fmt, frameRate / 4);
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
}
