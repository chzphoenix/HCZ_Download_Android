package com.huichongzi.download_android.download;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Created by cuihz on 14-7-25.
 */
class DBHelper extends OrmLiteSqliteOpenHelper {

    private static final Logger logger = LoggerFactory.getLogger(DBHelper.class);

    private static final String DB_NAME_SMARTCACHE = "ifeng.db";
    private static final int DB_VERSION_SMARTCACHE = 20140725;

    private static final Class<?>[] DATA_CLASSES = {DownloadInfo.class};

    // the DAO object we use to access the tables
    private Dao<DownloadInfo, String> downloadDao;

    public DBHelper(Context context) {
        super(context, DB_NAME_SMARTCACHE, null, DB_VERSION_SMARTCACHE);
    }

    private void openWAL(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            this.getWritableDatabase().enableWriteAheadLogging();
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            logger.info("isWriteAheadLoggingEnabled:{}",this.getWritableDatabase().isWriteAheadLoggingEnabled());
        }
    }

    /**
     * This is called when the database is first created. Usually you should call createTable
     * statements here to create the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            for (Class<?> dataClass : DATA_CLASSES) {
                logger.info("DBHelper.onCreate:{}",dataClass.getName());
                TableUtils.createTable(connectionSource, dataClass);
            }
//            openWAL();
        } catch (Throwable e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows
     * you to adjust the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            logger.info("db onUpgrade,dbname:{},oldVersion:{},newVersion:{}",DB_NAME_SMARTCACHE,oldVersion,newVersion);
            for (Class<?> dataClass : DATA_CLASSES) {
                logger.info("DBHelper.onUpgrade:{}",dataClass.getName());
                TableUtils.dropTable(connectionSource, dataClass, true);
            }
            onCreate(database, connectionSource);
        } catch (SQLException e) {
            logger.error(e.toString(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SmartCacheModel class. It will create it or just give the cached
     * value.
     */
    Dao<DownloadInfo, String> getDownloadDao() throws SQLException {
        if (null == downloadDao) {
            downloadDao = getDao(DownloadInfo.class);
        }
        return downloadDao;
    }

    @Override
    public void close() {
        downloadDao = null;
        super.close();
    }

}