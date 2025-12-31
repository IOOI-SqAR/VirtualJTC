package org.sqar.virtualjtc.jtcemu.base;

public interface ScreenControl {
    int DEFAULT_SCREEN_MARGIN = 20;
    int DEFAULT_SCREEN_REFRESH_MS = 50;
    int DEFAULT_SCREEN_SCALE = 3;

    String PROP_SCREEN_MARGIN = "screen.margin";
    String PROP_SCREEN_REFRESH_MS = "screen.refresh.ms";
    String PROP_SCREEN_SCALE = "screen.scale";

    void screenConfigChanged();

    void setScreenTextSelected(boolean state);

    int getScreenRefreshMillis();
}
