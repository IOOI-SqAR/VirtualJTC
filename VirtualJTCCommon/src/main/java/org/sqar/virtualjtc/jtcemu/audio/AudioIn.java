/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang)
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.jtcemu.base.AudioReader;
import org.sqar.virtualjtc.z8.Z8;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;


public abstract class AudioIn extends AudioIO implements AudioReader {
    protected int minValue;
    protected int maxValue;
    protected int sampleSizeInBits;
    protected int sampleSizeInBytes;
    protected boolean bigEndian;
    protected boolean dataSigned;

    private float thresholdRelValue;
    private int thresholdAbsValue;
    private volatile int selectedChannel;
    private int sampleBitMask;
    private int sampleSignMask;
    private int channels;
    private int adjustPeriodLen;
    private int adjustPeriodCnt;
    private int volStMinValue;
    private int volStMaxValue;
    private int volStDecPerFrame;
    private long begCycles;
    private long lastCycles;
    private long maxCycles;
    private long totalFrameCnt;
    private boolean firstCall;
    private boolean lastPhase;
    private boolean volumeStatus;


    protected AudioIn(
            AudioInFld audioInFld,
            Z8 z8,
            float thresholdRelValue) {
        super(audioInFld, z8);
        this.thresholdRelValue = thresholdRelValue;
        this.thresholdAbsValue = 0;
        this.volStDecPerFrame = 0;
        this.volStMinValue = 0;
        this.volStMaxValue = 0;
        this.minValue = 0;
        this.maxValue = 0;
        this.selectedChannel = 0;
        this.sampleBitMask = 0;
        this.sampleSignMask = 0;
        this.sampleSizeInBits = 0;
        this.sampleSizeInBytes = 0;
        this.channels = 0;
        this.adjustPeriodLen = 0;
        this.adjustPeriodCnt = 0;
        this.begCycles = 0;
        this.lastCycles = 0;
        this.maxCycles = 0;
        this.totalFrameCnt = 0;
        this.firstCall = true;
        this.lastPhase = false;
        this.bigEndian = false;
        this.dataSigned = false;
        this.volumeStatus = false;
    }


    protected abstract void checkOpenSource() throws IOException;

    protected abstract void closeSource();

    protected abstract byte[] readFrame()
            throws IOException, InterruptedException;


    protected void currentCycles(long totalCycles, long diffCycles) {
        // leer
    }


    public int getChannels() {
        return this.channels;
    }


    /*
     * Die Methode liest ein Frame und gibt das Samples
     * des ausgewaehlten Kanals zurueck.
     * Auch bei vorzeichenbehaftenen Audiodaten ist der Rueckgabewert
     * nicht negativ, da nur die betreffenden unteren Bits gefuellt sind.
     * Bei einem Rueckgabewert kleiner Null konnte kein Sample gelesen werden.
     */
    protected int readFrameAndGetSample()
            throws IOException, InterruptedException {
        int value = -1;
        byte[] frameData = readFrame();
        if (frameData != null) {
            int offset = this.selectedChannel * this.sampleSizeInBytes;
            if (offset + this.sampleSizeInBytes <= frameData.length) {
                value = 0;
                if (this.bigEndian) {
                    for (int i = 0; i < this.sampleSizeInBytes; i++) {
                        value = (value << 8) | ((int) frameData[offset + i] & 0xFF);
                    }
                } else {
                    for (int i = this.sampleSizeInBytes - 1; i >= 0; --i) {
                        value = (value << 8) | ((int) frameData[offset + i] & 0xFF);
                    }
                }
                value &= this.sampleBitMask;
            }
        }
        return value;
    }


    protected void setAudioFormat(AudioFormat fmt) {
        setAudioFormat(
                Math.round(fmt.getSampleRate()),
                fmt.getSampleSizeInBits(),
                fmt.getChannels(),
                fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED,
                fmt.isBigEndian());
    }


    protected void setAudioFormat(
            int frameRate,
            int sampleSizeInBits,
            int channels,
            boolean dataSigned,
            boolean bigEndian) {
        setAudioFormat(frameRate, sampleSizeInBits, channels);
        this.channels = channels;
        this.dataSigned = dataSigned;
        this.bigEndian = bigEndian;
        this.sampleSizeInBits = sampleSizeInBits;
        this.sampleSizeInBytes = (sampleSizeInBits + 7) / 8;
        this.sampleBitMask = ((1 << sampleSizeInBits) - 1);
        this.sampleSignMask = (1 << (sampleSizeInBits - 1));

        /*
         * Min-/Max-Regelung initialisieren
         *
         * Nach einer Periodenlaenge werden die Minimum- und Maximum-Werte
         * zueinander um einen Schritt angenaehert,
         * um so einen dynamischen Mittelwert errechnen zu koennen.
         */
        this.adjustPeriodLen = (int) this.frameRate / 256;
        if (this.adjustPeriodLen < 1) {
            this.adjustPeriodLen = 1;
        }
        this.adjustPeriodCnt = this.adjustPeriodLen;
        this.firstCall = true;

        // Wertebereich der Pegelanzeige
        int mask = (1 << sampleSizeInBits);
        if (this.dataSigned) {
            int maxLimit = (mask / 2) - 1;
            this.audioFld.fireSetVolumeLimits(-maxLimit, maxLimit);
        } else {
            this.audioFld.fireSetVolumeLimits(0, mask - 1);
        }

        // Werte fuer H/L-Erkennung
        calcAbsThresholdValue();
        this.volStDecPerFrame = 500000 / (frameRate + 10000);
    }


    public void setSelectedChannel(int channel) {
        this.selectedChannel = channel;
    }


    public void setThresholdValue(float thresholdValue) {
        this.thresholdRelValue = thresholdValue;
        calcAbsThresholdValue();
    }


    /* --- AudioReader --- */

    /*
     * Die Methode wird im CPU-Emulations-Thread aufgerufen
     * und liest die Phase des Toneingangs.
     */
    @Override
    public boolean readPhase() {
        int v = readSamples();
        if (v != -1) {
            int d = this.maxValue - this.minValue;
            if (this.lastPhase) {
                if (v < this.minValue + (d / 3)) {
                    this.lastPhase = false;
                }
            } else {
                if (v > this.maxValue - (d / 3)) {
                    this.lastPhase = true;
                }
            }
        }
        return this.lastPhase;
    }


    /*
     * Die Methode wird im CPU-Emulations-Thread aufgerufen
     * und gibt den Status zurueck, ob seit dem letzten Aufruf
     * eine Schwingung mit ueberwiegend einer hohen Amplitude anlag.
     */
    @Override
    public boolean readVolumeStatus() {
        readSamples();
        return this.volumeStatus;
    }


    /* --- private Methoden --- */

    private void calcAbsThresholdValue() {
        this.thresholdAbsValue = Math.round(
                this.thresholdRelValue
                        * (float) this.sampleBitMask);
    }


    /*
     * Die Methode liest die Samples seit dem letzten Aufruf
     * und gibt den Wert des letzten Samples zurueck.
     *
     * Rueckgabewert:
     *  -1: kein Wert gelesen
     */
    private int readSamples() {
        int rv = -1;
        try {
            if (this.stopRequested) {
                closeSource();
                checkFireFinished();
            } else {
                checkOpenSource();
                if (this.firstCall) {
                    this.firstCall = false;
                    this.begCycles = this.z8.getTotalCycles();
                    this.lastCycles = this.begCycles;
                    this.maxCycles = 0x7FFFFFFF00000000L / (this.frameRate + 1);
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
                                currentCycles(curCycles, diffCycles);

                                // bis zum naechsten auszuwertenden Frame lesen
                                int v = 0;
                                int i = nFrames;
                                do {
                                    v = readFrameAndGetSample();
                                    if (v != -1) {

                                        // dynamische Mittelwertbestimmung
                                        if (this.adjustPeriodCnt > 0) {
                                            --this.adjustPeriodCnt;
                                        } else {
                                            this.adjustPeriodCnt = this.adjustPeriodLen;
                                            if (this.minValue < this.maxValue) {
                                                this.minValue++;
                                            }
                                            if (this.maxValue > this.minValue) {
                                                --this.maxValue;
                                            }
                                        }

                                        // Wenn gelesener Wert negativ ist, Zahl korrigieren
                                        if (this.dataSigned
                                                && ((v & this.sampleSignMask) != 0)) {
                                            v |= ~this.sampleBitMask;
                                        }

                                        // Minimum-/Maximum-Werte und Mittelwert aktualisieren
                                        if (v < this.minValue) {
                                            this.minValue = v;
                                        } else if (v > this.maxValue) {
                                            this.maxValue = v;
                                        }
                                        rv = v;
                                    }
                                } while (--i > 0);

                                this.totalFrameCnt += nFrames;
                                this.lastCycles = curCycles;

                                /*
                                 * aktuellen Low/High-Status ermitteln
                                 *
                                 * Dazu werden wieder Minimum und Maximum ermitteln,
                                 * die allerdings wesentlich schneller konvergieren.
                                 * Ist die Differenz groesser als der Schwellwert,
                                 * ist der Status High.
                                 */
                                if (rv < this.volStMinValue) {
                                    this.volStMinValue = rv;
                                }
                                if (rv > this.volStMaxValue) {
                                    this.volStMaxValue = rv;
                                }
                                int d = this.volStMaxValue - this.volStMinValue;
                                this.volumeStatus = (d > this.thresholdAbsValue);
                                int volStAdjust = nFrames * this.volStDecPerFrame;
                                this.volStMinValue += volStAdjust;
                                this.volStMaxValue -= volStAdjust;

                                // Anzeige aktualisieren
                                this.audioFld.fireUpdVolume(
                                        rv > 0 ? rv : 0,
                                        this.volumeStatus);
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
        return rv;
    }
}
