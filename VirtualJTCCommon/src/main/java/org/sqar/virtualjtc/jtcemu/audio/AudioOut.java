/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Ausgangsseitige Emulation
 * des Kassettenrecorderanschlusses und des Lautsprechers
 *
 * Die Ausgabe erfolgt als Rechteckkurve
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.jtcemu.base.AudioWriter;
import org.sqar.virtualjtc.z8.Z8;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;


public class AudioOut extends AudioIO implements AudioWriter {
    public static final int PHASE_0_VALUE = 0;
    public static final int PHASE_1_VALUE = 200;

    private boolean lineRequested;
    private boolean firstCall;
    private boolean firstPhase;
    private boolean recordingEnabled;
    private long begCycles;
    private long lastCycles;
    private long maxCycles;
    private long totalFrameCnt;
    private int audioPos;
    private byte[] audioBuf;
    private Mixer mixer;
    private SourceDataLine dataLine;
    private AudioData recordedData;


    protected AudioOut(
            AudioOutFld audioOutFld,
            Z8 z8,
            int frameRate,
            boolean lineRequested,
            Mixer mixer,
            boolean record) {
        super(audioOutFld, z8);
        this.lineRequested = lineRequested;
        this.mixer = mixer;
        this.firstCall = true;
        this.recordingEnabled = false;
        this.begCycles = 0;
        this.lastCycles = 0;
        this.maxCycles = 0;
        this.totalFrameCnt = 0;
        this.audioPos = 0;
        this.audioBuf = null;
        this.dataLine = null;
        this.recordedData = null;

        if (record) {
            setAudioFormat(frameRate > 0 ? frameRate : 44100, 1, 1);
            this.recordedData = new AudioData(this.frameRate);
        } else {
            if (this.lineRequested) {
                if (frameRate > 0) {
                    setAudioFormat(frameRate, 1, 1);
                }
            } else {
                requestStop();
            }
        }
    }


    public synchronized void closeCPUSynchronLine() {
        if ((this.dataLine != null) && isCPUSynchronLine(this.dataLine)) {
            this.lineRequested = false;
            closeDataLine(this.dataLine);
            this.dataLine = null;
            checkFireFinished();
        }
    }


    public AudioData getRecordedData() {
        return this.recordedData;
    }


    public String getRecordedDurationText() {
        return this.recordedData != null ?
                getTimeText(
                        this.frameRate,
                        this.recordedData.getSampleCount())
                : "";
    }


    /* --- AudioWriter --- */

    /*
     * Die Methode wird im CPU-Emulations-Thread aufgerufen
     * und besagt, dass am entsprechenden Ausgabetor ein Wert anliegt.
     */
    @Override
    public void writePhase(boolean phase) {
        try {
            if (this.stopRequested) {
                this.lineRequested = false;
                if (this.dataLine != null) {
                    closeDataLine(this.dataLine);
                    this.dataLine = null;
                }
                if (this.recordedData != null) {
                    // 100 ms Pause am Ende
                    if (this.recordingEnabled) {
                        this.recordedData.finish(this.frameRate / 10);
                        this.recordingEnabled = false;
                    }
                }
                checkFireFinished();
            } else {
                if (this.lineRequested && (this.dataLine == null)) {
                    this.dataLine = openSourceDataLine();
                    this.lineRequested = false;
                }
            if (this.firstCall) {
                this.firstCall = false;
                    this.firstPhase = phase;
                    this.begCycles = this.z8.getTotalCycles();
                    this.lastCycles = this.begCycles;
                    this.maxCycles = 0x7FFFFFFF00000000L / (this.frameRate + 1);
                    this.audioFld.fireSetVolumeLimits(0, 0xFF);
            } else {
                    long curCycles = this.z8.getTotalCycles();
                    long allCycles = curCycles - this.begCycles;
                    if ((allCycles < 0) || (allCycles > this.maxCycles)) {
                        requestStop();
                    } else {
                        int nFrames = (int) ((allCycles * this.frameRate
                                / this.cyclesPerSecond)
                                - this.totalFrameCnt);
                        if (nFrames > 0) {
                            long diffCycles = curCycles - this.lastCycles;
                        if (diffCycles > 0) {
                                if (this.dataLine != null) {
                                    this.z8.setSpeedUnlimitedFor((int) (diffCycles * 8));
                                }
                                writeSamples(nFrames, phase);
                                this.totalFrameCnt += nFrames;
                                this.lastCycles = curCycles;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            setErrorText(ex.getMessage());
            requestStop();
        } catch (Exception ex) {
                                    /*
             * z.B. InterruptedException bei Programmbeendigung oder
             * eine andere Exception bei Abziehen eines aktiven USB-Audiogeraetes
             */
            requestStop();
        }
    }


    /* --- private Methoden --- */

    private SourceDataLine openSourceDataLine() throws IOException {
        SourceDataLine line = null;
        if (this.frameRate > 0) {
            line = openSourceDataLine(this.frameRate);
        } else {
            for (int i = 0; i < preferredLineFrameRates.length; i++) {
                line = openSourceDataLine(preferredLineFrameRates[i]);
                if (line != null) {
                    break;
                }
            }
        }
        if (line != null) {
            if (this.frameRate == 0) {
                /*
                 * Der Audiokanal selbst hat zwar 8 Bit,
                 * aber der logische Informationsgehalt nur 1 Bit.
                 * Deshalb soll auch nur 1 Bit angezeigt werden.
                                     */
                setAudioFormat(
                        Math.round(line.getFormat().getSampleRate()), 1, 1);
            }
            this.dataLine = line;

            /*
             * externer Audiopuffer anlegen
             *
             * Damit die Implementierung des Blockens ausserhalb
             * der SourceDataLine.write-Methode funktioniert,
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


    private SourceDataLine openSourceDataLine(
            int frameRate) throws IOException {
        return openSourceDataLine(
                new AudioFormat((float) frameRate, 8, 1, false, false),
                this.mixer);
    }


    private void writeSamples(
            int nSamples,
            boolean phase)
            throws IOException, InterruptedException {
        int value = (phase ? PHASE_1_VALUE : PHASE_0_VALUE);
        this.audioFld.fireUpdVolume(value, false);

        // Daten in Audiokanal schreiben
        SourceDataLine line = this.dataLine;
        byte[] audioBuf = this.audioBuf;
        if ((line != null) && (audioBuf != null) && (nSamples > 0)) {
            for (int i = 0; i < nSamples; i++) {
                if (this.audioPos >= audioBuf.length) {
                    if (line.available() < audioBuf.length) {
                        int n = 0;
                        do {
                            Thread.sleep(10);
                            n++;
                            if (n > 100) {
                                closeDataLine(line);
                                line = null;
                                this.dataLine = null;
                                setErrorText(ERROR_LINE_CLOSED_BECAUSE_NOT_WORKING);
                                requestStop();
                                break;
                            }
                        } while (line.available() < audioBuf.length);
                    }
                    if (line != null) {
                        if (this.stopRequested) {
                            line.flush();
                        } else {
                            line.write(audioBuf, 0, audioBuf.length);
                                    }
                                }
                    this.audioPos = 0;
                            }
                audioBuf[this.audioPos++] = (byte) value;
                        }
                    }

        // Daten aufzeichen
        if (this.recordedData != null) {
            // Aufzeichnung mit dem ersten Phasenwechsel beginnen
            if (phase != this.firstPhase) {
                this.recordingEnabled = true;
                }
            if (this.recordingEnabled) {
                this.recordedData.addSamples(nSamples, phase);
            }
        }
    }
}
