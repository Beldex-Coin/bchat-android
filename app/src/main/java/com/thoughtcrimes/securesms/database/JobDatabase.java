package com.thoughtcrimes.securesms.database;

import android.content.Context;

import com.thoughtcrimes.securesms.database.helpers.SQLCipherOpenHelper;

public class JobDatabase extends Database {

  public static final String[] CREATE_TABLE = new String[] { Jobs.CREATE_TABLE,
                                                             Constraints.CREATE_TABLE,
                                                             Dependencies.CREATE_TABLE };

  public static final class Jobs {
    public  static final String TABLE_NAME            = "job_spec";
    private static final String ID                    = "_id";
    private static final String JOB_SPEC_ID           = "job_spec_id";
    private static final String FACTORY_KEY           = "factory_key";
    private static final String QUEUE_KEY             = "queue_key";
    private static final String CREATE_TIME           = "create_time";
    private static final String NEXT_RUN_ATTEMPT_TIME = "next_run_attempt_time";
    private static final String RUN_ATTEMPT           = "run_attempt";
    private static final String MAX_ATTEMPTS          = "max_attempts";
    private static final String MAX_BACKOFF           = "max_backoff";
    private static final String MAX_INSTANCES         = "max_instances";
    private static final String LIFESPAN              = "lifespan";
    private static final String SERIALIZED_DATA       = "serialized_data";
    private static final String IS_RUNNING            = "is_running";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + ID                    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                                                    JOB_SPEC_ID           + " TEXT UNIQUE, " +
                                                                                    FACTORY_KEY           + " TEXT, " +
                                                                                    QUEUE_KEY             + " TEXT, " +
                                                                                    CREATE_TIME           + " INTEGER, " +
                                                                                    NEXT_RUN_ATTEMPT_TIME + " INTEGER, " +
                                                                                    RUN_ATTEMPT           + " INTEGER, " +
                                                                                    MAX_ATTEMPTS          + " INTEGER, " +
                                                                                    MAX_BACKOFF           + " INTEGER, " +
                                                                                    MAX_INSTANCES         + " INTEGER, " +
                                                                                    LIFESPAN              + " INTEGER, " +
                                                                                    SERIALIZED_DATA       + " TEXT, " +
                                                                                    IS_RUNNING            + " INTEGER)";
  }

  public static final class Constraints {
    public  static final String TABLE_NAME  = "constraint_spec";
    private static final String ID          = "_id";
    private static final String JOB_SPEC_ID = "job_spec_id";
    private static final String FACTORY_KEY = "factory_key";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                                                    JOB_SPEC_ID + " TEXT, " +
                                                                                    FACTORY_KEY + " TEXT, " +
                                                                                    "UNIQUE(" + JOB_SPEC_ID + ", " + FACTORY_KEY + "))";
  }

  public static final class Dependencies {
    public  static final String TABLE_NAME             = "dependency_spec";
    private static final String ID                     = "_id";
    private static final String JOB_SPEC_ID            = "job_spec_id";
    private static final String DEPENDS_ON_JOB_SPEC_ID = "depends_on_job_spec_id";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + ID                     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                                                                    JOB_SPEC_ID            + " TEXT, " +
                                                                                    DEPENDS_ON_JOB_SPEC_ID + " TEXT, " +
                                                                                    "UNIQUE(" + JOB_SPEC_ID + ", " + DEPENDS_ON_JOB_SPEC_ID + "))";
  }


  public JobDatabase(Context context, SQLCipherOpenHelper databaseHelper) {
    super(context, databaseHelper);
  }
}
