package com.fexed.coffeecounter;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by fexed on 04/12/2017.
 */

@Database(entities = {Coffeetype.class, Cup.class}, version = 22)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CoffeetypeDAO coffetypeDao();
    public abstract CupDAO cupDAO();
}
