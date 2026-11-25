package lab7.client;

import lab7.net.StatisticsPacket;

import java.util.List;

public record CameraStatistics(int webCameras, int photoCameras, int smartphones,
                               List<StatisticsPacket.Reading> readings) {
}
