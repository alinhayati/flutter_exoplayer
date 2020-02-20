package danielr2001.audioplayer.audioplayers;

public class ForegroundPlayParams {
    public String getSmallIconFileName() {
        return smallIconFileName;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getLargeIconUrl() {
        return largeIconUrl;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public int getNotificationDefaultActionsInt() {
        return notificationDefaultActionsInt;
    }

    public int getNotificationActionCallbackModeInt() {
        return notificationActionCallbackModeInt;
    }

    public int getNotificationCustomActionsInt() {
        return notificationCustomActionsInt;
    }

    private String smallIconFileName, title, subTitle, largeIconUrl;
    private boolean isLocal;
    private int notificationDefaultActionsInt, notificationActionCallbackModeInt, notificationCustomActionsInt;

    public ForegroundPlayParams(String smallIconFileName, String title, String subTitle,
                                String largeIconUrl, boolean isLocal,
                                int notificationDefaultActionsInt,
                                int notificationActionCallbackModeInt,
                                int notificationCustomActionsInt) {
        this.smallIconFileName = smallIconFileName;
        this.title = title;
        this.subTitle = subTitle;
        this.largeIconUrl = largeIconUrl;
        this.isLocal = isLocal;
        this.notificationDefaultActionsInt = notificationDefaultActionsInt;
        this.notificationActionCallbackModeInt = notificationActionCallbackModeInt;
        this.notificationCustomActionsInt = notificationCustomActionsInt;
    }
}
