package com.example.sensorplaytest3;

/** Class that manages the level number.
 *  Level has to be a class variable because the GameView Class resets after completing one level and therefore it would reset the level number if it was a object variable.
 *  Because with each activity reset, all objects are getting newly created.
 */
public class nextLevel {
    private static int level = 1;


    public static int proceed(){
        return (level++);
    }
    public static int getLevel(){
        return level;
    }

    public static void resetLevel() {
        nextLevel.level = 0;
    }
}
