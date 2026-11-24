package lab7.net;

import java.io.Serializable;

public record LogResult(int changes, int objectCount, int propertiesRead, int propertiesMissing, String[] warnings)
        implements Serializable {
}
