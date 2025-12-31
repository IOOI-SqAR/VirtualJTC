/*
 * (c) 2016-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basiskomponente fuer die Emulation des Kassettenrecorderanschlusses
 * und des Lautsprechers
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.base.JTCSys;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;


public abstract class AbstractAudioIOFld extends JPanel {
    public static class MixerItem {
        private String name;
        private Mixer.Info info;

        public MixerItem(String name, Mixer.Info info) {
            this.name = name;
            this.info = info;
        }

        public Mixer.Info getMixerInfo() {
            return this.info;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    ;


    public static final String PROP_ENABLED = "enabled";
    public static final String PROP_FRAME_RATE = "frame.rate";
    public static final String PROP_MIXER_NAME = "mixer.name";

    protected static final String FILE_GROUP_AUDIO = "audio";


    protected AudioFrm audioFrm;
    protected JTCSys jtcSys;
    protected String propPrefix;
    protected JLabel labelFrameRate;
    protected JLabel labelMixer;
    protected JComboBox<Object> comboFrameRate;
    protected JComboBox<Object> comboMixer;
    protected VolumeBar volumeBar;


    private static final int[] frameRates = {
            96000, 48000, 44100, 45454,
            32000, 22050, 16000, 11025,
            8000};

    private static FileFilter fileFilter = null;

    private Timer progressTimer;


    protected AbstractAudioIOFld(
            AudioFrm audioFrm,
            JTCSys jtcSys,
            String propPrefix) {
        this.audioFrm = audioFrm;
        this.jtcSys = jtcSys;
        this.propPrefix = propPrefix;
        this.volumeBar = new VolumeBar();
        this.labelFrameRate = new JLabel("Abtastrate (Hz):"); // TODO: i18n
        this.labelMixer = new JLabel("Ger\u00E4t:"); // TODO: i18n

        this.comboFrameRate = new JComboBox<>();
        this.comboFrameRate.setEditable(false);
        this.comboFrameRate.addItem("Standard"); // TODO: i18n
        for (int frameRate : frameRates) {
            this.comboFrameRate.addItem(Integer.valueOf(frameRate));
        }

        this.comboMixer = new JComboBox<>();
        this.comboMixer.setEditable(false);
        appendMixerItems();

        this.progressTimer = new Timer(
                200,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        updProgressFld();
                    }
                });
    }


    protected void applySettings() {
        if (this.comboMixer.isEnabled()) {
            String mixerName = AppContext.getProperty(
                    this.propPrefix + PROP_MIXER_NAME);
            if (mixerName != null) {
                int n = this.comboMixer.getItemCount();
                for (int i = 0; i < n; i++) {
                    Object item = this.comboMixer.getItemAt(i);
                    if (item != null) {
                        if (mixerName.equals(item.toString())) {
                            try {
                                this.comboMixer.setSelectedIndex(i);
                            } catch (IllegalArgumentException ex) {
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (this.comboFrameRate.isEnabled()) {
            Integer frameRate = AppContext.getIntegerProperty(
                    this.propPrefix + PROP_FRAME_RATE);
            if (frameRate != null) {
                this.comboFrameRate.setSelectedItem(frameRate);
            }
        }
    }


    protected void audioFinished(String errMsg) {
        stopProgressTimer();
    }


    public void checkEnable() {
        if (AppContext.getBooleanProperty(
                this.propPrefix + PROP_ENABLED,
                false)) {
            doEnable();
            this.audioFrm.setSelectedComponent(this);
        }
    }


    protected abstract void doEnable();


    public void fireAudioFinished(final AudioIO audioIO) {
        if ((audioIO != null) && (audioIO == getAudioIO())) {
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            audioFinished(audioIO.getErrorText());
                        }
                    });
        }
    }


    public void fireFormatChanged(
            final AudioIO audioIO,
            final String formatText) {
        if ((audioIO != null) && (audioIO == getAudioIO())) {
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            formatChanged(formatText);
                        }
                    });
        }
    }


    public void fireMonitorStatusChanged(
            final AudioIO audioIO,
            final boolean status,
            final String errorText) {
        if ((audioIO != null) && (audioIO == getAudioIO())) {
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            monitorStatusChanged(status, errorText);
                        }
                    });
        }
    }


    public void fireSetVolumeLimits(final int minLimit, final int maxLimit) {
        final VolumeBar volumeVar = this.volumeBar;
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        volumeVar.setVolumeLimits(minLimit, maxLimit);
                    }
                });
    }


    public void fireUpdVolume(final int value, boolean volumeStatus) {
        final VolumeBar volumeBar = this.volumeBar;
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        volumeBar.updVolume(value);
                    }
                });
    }


    protected void formatChanged(String formatText) {
        // leer
    }


    protected AudioIO getAudioIO() {
        return null;
    }


    protected static FileFilter getFileFilter() {
        if (fileFilter == null) {
            try {
                Set<String> extensions = new TreeSet<>();
                extensions.add("csw");
                for (AudioFileFormat.Type t : AudioSystem.getAudioFileTypes()) {
                    String ext = t.getExtension();
                    if (ext != null) {
                        int len = ext.length();
                        if (ext.startsWith(".") && (len > 1)) {
                            extensions.add(ext.substring(1));
                        } else if (len > 0) {
                            extensions.add(ext);
                        }
                    }
                }
                StringBuilder buf = new StringBuilder(128);
                for (String ext : extensions) {
                    if (buf.length() > 0) {
                        buf.append("; *.");
                    } else {
                        buf.append("Audio-/Tape-Dateien (*."); // TODO: i18n
                    }
                    buf.append(ext);
                }
                buf.append(')');
                fileFilter = new FileNameExtensionFilter(
                        buf.toString(),
                        extensions.toArray(
                                new String[extensions.size()]));
            } catch (ArrayStoreException ex) {
            } catch (IllegalArgumentException ex) {
            }
        }
        return fileFilter;
    }


    protected abstract Line.Info[] getLineInfo(Mixer mixer);


    protected int getSelectedFrameRate() {
        int idx = this.comboFrameRate.getSelectedIndex() - 1;
        return (idx >= 0) && (idx < frameRates.length) ?
                frameRates[idx]
                : 0;
    }


    protected Mixer getSelectedMixer() throws IOException {
        Mixer mixer = null;
        Object obj = this.comboMixer.getSelectedItem();
        if (obj != null) {
            if (obj instanceof MixerItem) {
                try {
                    mixer = AudioSystem.getMixer(((MixerItem) obj).getMixerInfo());
                } catch (IllegalArgumentException ex) {
                    throw new IOException(
                            "Ausgew\u00E4hltes Audioger\u00E4t nicht verf\u00FCgbar"); // TODO: i18n
                }
            }
        }
        return mixer;
    }


    protected void memorizeSettings() {
        String mixerName = "";
        Object mixerItem = this.comboMixer.getSelectedItem();
        if (mixerItem != null) {
            if (mixerItem instanceof MixerItem) {
                mixerName = ((MixerItem) mixerItem).toString();
            }
        }
        AppContext.setProperty(this.propPrefix + PROP_MIXER_NAME, mixerName);

        String frameRate = "";
        Object rateItem = this.comboFrameRate.getSelectedItem();
        if (rateItem != null) {
            if (rateItem instanceof Number) {
                frameRate = rateItem.toString();
            }
        }
        AppContext.setProperty(this.propPrefix + PROP_FRAME_RATE, frameRate);
    }


    protected void monitorStatusChanged(boolean status, String errorText) {
        // leer
    }


    protected void refreshMixerList() {
        if (this.comboMixer.isEnabled()) {
            this.comboMixer.removeAllItems();
            appendMixerItems();
        }
    }


    protected void showErrorDlg(String msg) {
        if (msg != null) {
            JOptionPane.showMessageDialog(
                    this,
                    msg,
                    "Fehler", // TODO: i18n
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    protected void startProgressTimer() {
        if (!this.progressTimer.isRunning())
            this.progressTimer.start();
    }


    protected void stopProgressTimer() {
        if (this.progressTimer.isRunning())
            this.progressTimer.stop();
    }


    protected abstract void updProgressFld();


    /* --- private Methoden --- */

    private void appendMixerItems() {
        this.comboMixer.addItem("Standard"); // TODO: i18n
        appendMixerItems(false);
        if (this.comboMixer.getItemCount() < 2) {
            appendMixerItems(false);
        }
    }


    private void appendMixerItems(boolean ports) {
        for (Mixer.Info mInfo : AudioSystem.getMixerInfo()) {
            try {
                boolean matched = false;
                Line.Info[] infoArray = getLineInfo(AudioSystem.getMixer(mInfo));
                if (infoArray != null) {
                    for (Line.Info lInfo : infoArray) {
                        if (ports == (lInfo instanceof Port.Info)) {
                            matched = true;
                            break;
                        }
                    }
                }
                if (matched) {
                    String s = mInfo.getName();
                    if (s != null) {
                        if (s.isEmpty()) {
                            s = null;
                        }
                    }
                    if (s == null) {
                        s = "Unbekanntes Ger\u00E4t"; // TODO: i18n
                    }
                    this.comboMixer.addItem(new MixerItem(s, mInfo));
                }
            } catch (Exception ex) {
            }
        }
    }
}
