package lab6.client.view;

public class CommandLineOptions {
    public static final String HELP = "help";
    public static final String INTERACTIVE = "interactive";
    public static final String SHOW = "show";
    public static final String WATCH = "watch";
    public static final String LOG = "log";
    public static final String REPLACE = "replace";
    public static final int DEFAULT_INTERVAL = 5;

    private final String action;
    private final String fileName;
    private final String sourceName;
    private final String targetName;
    private final String csvFileName;
    private final int interval;

    public CommandLineOptions(String action, String fileName, String sourceName, String targetName, String csvFileName, int interval) {
        this.action = action;
        this.fileName = fileName;
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.csvFileName = csvFileName;
        this.interval = interval;
    }

    public String getAction() {
        return action;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getCsvFileName() {
        return csvFileName;
    }

    public int getInterval() {
        return interval;
    }
}
