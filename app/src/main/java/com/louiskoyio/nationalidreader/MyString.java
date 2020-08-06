package com.louiskoyio.nationalidreader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyString{
    private static final float THRESHOLD = (float) 0.75;

    private final Logger logger = Logger.getLogger(MyString.class.getName());

    private String compareWord,unwantedWord;
    private Map <Character, Integer> strMap;

    public MyString(String compareWord){ //java.lang.String is final...
        this.compareWord = compareWord;
        this.unwantedWord = unwantedWord;
        this.strMap = this.generateCharMap(compareWord);
    }

    @Override
    public String toString(){
        return this.compareWord;
    }

    private Map <Character, Integer> generateCharMap(String str){
        Map <Character, Integer> map = new HashMap<>();
        Integer currentChar;
        for(char c: str.toCharArray()){
            currentChar = map.get(c);
            if(currentChar == null){
                map.put(c, 1);
            } else {
                map.put(c, currentChar+1);
            }
        }
        return map;
    }

    public boolean isSimilar(String wordBeingCompared){
        Map <Character, Integer> compareStrMap = this.generateCharMap(wordBeingCompared);
        Set <Character> charSet = compareStrMap.keySet();
        int similarChars = 0;
        int totalStrChars = this.compareWord.length();
        float thisThreshold;

        if(totalStrChars < compareStrMap.size()){
            totalStrChars = wordBeingCompared.length();
        }

        Iterator it = charSet.iterator();
        char currentChar;
        Integer currentCountStrMap;
        Integer currentCountCompareStrMap;
        while(it.hasNext()){
            currentChar = (Character)it.next();
            currentCountStrMap = strMap.get(currentChar);
            if(currentCountStrMap != null){
                currentCountCompareStrMap = compareStrMap.get(currentChar);
                if (currentCountCompareStrMap >= currentCountStrMap){
                    similarChars += currentCountStrMap;
                } else {
                    similarChars += currentCountCompareStrMap;
                }
            }
        }

        thisThreshold = ((float) similarChars)/((float) totalStrChars);
        Logger.getLogger(MyString.class.getName()).log(Level.INFO, "similarChars: {0}, totalStrChars: {1}, thisThreshold: {2}", new Object[]{similarChars, totalStrChars, thisThreshold});
        if(thisThreshold > THRESHOLD){
            return true;
        }
        return false;
    }
}