package com.fexed.coffeecounter.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.fexed.coffeecounter.data.Coffeetype;

import java.util.List;

/**
 * Created by Federico Matteoni on 04/12/2017.
 */
@Dao
public interface CoffeetypeDAO {
    @Query("SELECT * FROM coffeetype")
    List<Coffeetype> getAll();

    //1 = true, 0 = false
    @Query("SELECT * FROM coffeetype WHERE fav = 1")
    List<Coffeetype> getFavs();

    @Query("SELECT * FROM coffeetype WHERE coffeetype.`key` IS :key")
    Coffeetype get(int key);

    @Insert
    void insertAll(Coffeetype... types);

    @Insert
    void insert(Coffeetype type);

    @Delete
    void delete(Coffeetype type);

    @Delete
    void deleteAll(Coffeetype... type);

    @Update
    void update(Coffeetype... types);

    @Query("DELETE FROM coffeetype")
    void nuke();

    @RawQuery
    int checkpoint(SupportSQLiteQuery supportSQLiteQuery);
}
