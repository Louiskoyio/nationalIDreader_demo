package com.louiskoyio.nationalidreader.utilities;

import java.util.ArrayList;
import java.util.List;

public interface ImageProcessorInterface {

    Boolean isWordValid(String word);
    Boolean lineWithMoreThanTwoWords(String line);
    String cleanupNumber(String text);
    Boolean containsDot(String line);
    Boolean containsNumber(String line);
    List<String> validNumbers(List<String> allNumbers);
}
