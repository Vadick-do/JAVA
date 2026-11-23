package lab7.net;

import lab7.model.FileReadResult;

import java.io.Serializable;

public record LogResult(FileReadResult report, int changes) implements Serializable {
}
