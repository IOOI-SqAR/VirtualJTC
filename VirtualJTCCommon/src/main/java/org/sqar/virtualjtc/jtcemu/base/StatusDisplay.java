package org.sqar.virtualjtc.jtcemu.base;

public interface StatusDisplay {

    int STATUS_REFRESH_MILLIS = 750;
    int STATUS_SHOW_MSG_MILLIS = 5000;
    String DEFAULT_STATUS_TEXT = "Emulator l\u00E4uft..."; // TODO: i18n


    void showStatusText(String text);
}
