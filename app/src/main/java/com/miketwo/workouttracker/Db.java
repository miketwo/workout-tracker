package com.miketwo.workouttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

final class Db extends SQLiteOpenHelper {
    static final String NAME = "workouts.db";
    private static Db instance;

    static synchronized Db get(Context context) {
        if (instance == null) instance = new Db(context.getApplicationContext());
        return instance;
    }

    static synchronized void resetForTests() { if (instance != null) instance.close(); instance = null; }

    private Db(Context context) { super(context, NAME, null, 5); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE plans (id INTEGER PRIMARY KEY, name TEXT NOT NULL, weekday INTEGER NOT NULL DEFAULT 1, type TEXT NOT NULL DEFAULT 'Strength', notes TEXT NOT NULL DEFAULT '', archived INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY, plan_id INTEGER NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL, sets INTEGER NOT NULL DEFAULT 3, reps INTEGER NOT NULL DEFAULT 8, weight REAL NOT NULL DEFAULT 0, unit TEXT NOT NULL DEFAULT 'lb', duration_seconds INTEGER NOT NULL DEFAULT 0, distance REAL NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', rest_seconds INTEGER NOT NULL DEFAULT 90, bodyweight INTEGER NOT NULL DEFAULT 0, assistance INTEGER NOT NULL DEFAULT 0, load_mode TEXT NOT NULL DEFAULT 'standard', muscle_group TEXT NOT NULL DEFAULT '', FOREIGN KEY(plan_id) REFERENCES plans(id))");
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY, plan_id INTEGER, plan_name TEXT NOT NULL, type TEXT NOT NULL, started_at TEXT NOT NULL, ended_at TEXT, status TEXT NOT NULL DEFAULT 'active', notes TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE set_results (id INTEGER PRIMARY KEY, session_id INTEGER NOT NULL, exercise_id INTEGER, exercise_name TEXT NOT NULL, exercise_position INTEGER NOT NULL, set_number INTEGER NOT NULL, target_reps INTEGER NOT NULL, target_weight REAL NOT NULL, target_duration_seconds INTEGER NOT NULL DEFAULT 0, actual_reps INTEGER NOT NULL, actual_weight REAL NOT NULL, actual_duration_seconds INTEGER NOT NULL DEFAULT 0, rir INTEGER NOT NULL DEFAULT -1, status TEXT NOT NULL DEFAULT 'complete', created_at TEXT NOT NULL, FOREIGN KEY(session_id) REFERENCES sessions(id))");
        createProfileAndSessionTables(db);
        addResultColumns(db);
        createPackingItemsTable(db);
        seedPackingItems(db);
        db.execSQL("CREATE TABLE cardio (id INTEGER PRIMARY KEY, activity TEXT NOT NULL, date TEXT NOT NULL, duration_min REAL NOT NULL DEFAULT 0, distance REAL NOT NULL DEFAULT 0, unit TEXT NOT NULL DEFAULT 'mi', intervals TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', laps INTEGER NOT NULL DEFAULT 0, pool_length REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_results_session ON set_results(session_id)");
        db.execSQL("CREATE INDEX idx_results_exercise ON set_results(exercise_name)");
        seed(db);
    }

    private void seed(SQLiteDatabase db) {
        long upper = addPlan(db, "Upper body", DayOfWeek.MONDAY.getValue(), "Strength", "A straightforward upper-body machine session.");
        addExercise(db, upper, "Lat pulldown", 0, 3, 8, 100, "lb", "Adjust the seat so thighs are secure.");
        addExercise(db, upper, "Incline chest press", 1, 3, 8, 90, "lb", "Record the seat position here after the first session.");
        addExercise(db, upper, "Seated row", 2, 3, 8, 80, "lb", "Keep chest tall.");

        long lower = addPlan(db, "Lower body", DayOfWeek.WEDNESDAY.getValue(), "Strength", "Machine-focused lower-body session.");
        addExercise(db, lower, "Leg press", 0, 3, 8, 140, "lb", "Use a comfortable foot position.");
        addExercise(db, lower, "Leg curl", 1, 3, 8, 70, "lb", "Record machine settings after the first session.");
        addExercise(db, lower, "Calf raise", 2, 3, 10, 80, "lb", "Full comfortable range.");

        addPlan(db, "Treadmill run", DayOfWeek.FRIDAY.getValue(), "Run", "Run/walk intervals; enter totals afterward in this release.");
        addPlan(db, "Pool swim", DayOfWeek.SATURDAY.getValue(), "Swim", "Count lengths and enter the session afterward.");
        seedExistingHistory(db, upper);
    }

    private void seedExistingHistory(SQLiteDatabase db, long upperPlan) {
        ContentValues session = new ContentValues();
        session.put("plan_id", upperPlan); session.put("plan_name", "Upper body"); session.put("type", "Strength");
        session.put("started_at", "2026-07-09T18:00:00"); session.put("ended_at", "2026-07-09T19:00:00"); session.put("status", "complete");
        session.put("notes", "Imported from the original CSV log.");
        long sessionId = db.insertOrThrow("sessions", null, session);
        seedResult(db, sessionId, "Lat pulldown", 0, 1, 8, 100);
        seedResult(db, sessionId, "Lat pulldown", 0, 2, 8, 100);
        seedResult(db, sessionId, "Lat pulldown", 0, 3, 8, 100);
        seedResult(db, sessionId, "Incline chest press", 1, 1, 6, 90);
        seedResult(db, sessionId, "Incline chest press", 1, 2, 6, 90);
        seedResult(db, sessionId, "Incline chest press", 1, 3, 6, 90);
        ContentValues cardio = new ContentValues();
        cardio.put("activity", "Run"); cardio.put("date", "2026-07-09"); cardio.put("duration_min", 26); cardio.put("distance", 2); cardio.put("unit", "mi");
        cardio.put("intervals", "Roughly 6 min jog / 2 min walk"); cardio.put("notes", "Estimated from memory; imported from the original CSV log.");
        db.insertOrThrow("cardio", null, cardio);
    }

    private void seedResult(SQLiteDatabase db, long sessionId, String exercise, int position, int set, int reps, double weight) {
        ContentValues v = new ContentValues();
        v.put("session_id", sessionId); v.put("exercise_name", exercise); v.put("exercise_position", position); v.put("set_number", set);
        v.put("target_reps", reps); v.put("target_weight", weight); v.put("actual_reps", reps); v.put("actual_weight", weight);
        v.put("rir", -1); v.put("status", "complete"); v.put("created_at", "2026-07-09T18:00:00");
        db.insertOrThrow("set_results", null, v);
    }

    private long addPlan(SQLiteDatabase db, String name, int weekday, String type, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("weekday", weekday); v.put("type", type); v.put("notes", notes);
        return db.insertOrThrow("plans", null, v);
    }

    private void addExercise(SQLiteDatabase db, long plan, String name, int position, int sets, int reps, double weight, String unit, String notes) {
        ContentValues v = new ContentValues();
        v.put("plan_id", plan); v.put("name", name); v.put("position", position); v.put("sets", sets); v.put("reps", reps); v.put("weight", weight); v.put("unit", unit); v.put("notes", notes); v.put("muscle_group", inferMuscleGroup(name));
        db.insertOrThrow("exercises", null, v);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) { createPackingItemsTable(db); seedPackingItems(db); }
        if (oldVersion < 3) { createProfileAndSessionTables(db); }
        if (oldVersion < 4) { addResultColumns(db); }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN load_mode TEXT NOT NULL DEFAULT 'standard'");
            db.execSQL("UPDATE exercises SET load_mode='counterbalanced' WHERE assistance=1 OR unit LIKE 'assisted%'");
        }
    }
    private void createProfileAndSessionTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS profile (id INTEGER PRIMARY KEY CHECK(id=1), body_weight REAL NOT NULL DEFAULT 0, unit TEXT NOT NULL DEFAULT 'lb')");
        db.execSQL("CREATE TABLE IF NOT EXISTS session_exercises (id INTEGER PRIMARY KEY, session_id INTEGER NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL, sets INTEGER NOT NULL DEFAULT 3, reps INTEGER NOT NULL DEFAULT 8, weight REAL NOT NULL DEFAULT 0, unit TEXT NOT NULL DEFAULT 'lb', duration_seconds INTEGER NOT NULL DEFAULT 0, notes TEXT NOT NULL DEFAULT '', rest_seconds INTEGER NOT NULL DEFAULT 90, bodyweight INTEGER NOT NULL DEFAULT 0, load_mode TEXT NOT NULL DEFAULT 'standard', muscle_group TEXT NOT NULL DEFAULT '', FOREIGN KEY(session_id) REFERENCES sessions(id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_session_exercises_session ON session_exercises(session_id,position)");
    }
    private void addResultColumns(SQLiteDatabase db) {
        try { db.execSQL("ALTER TABLE set_results ADD COLUMN raw_weight REAL NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE set_results ADD COLUMN raw_unit TEXT NOT NULL DEFAULT 'lb'"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE set_results ADD COLUMN effective_weight_lb REAL NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE set_results ADD COLUMN load_mode TEXT NOT NULL DEFAULT 'standard'"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE set_results ADD COLUMN body_weight_snapshot REAL NOT NULL DEFAULT 0"); } catch (Exception ignored) {}
        db.execSQL("UPDATE set_results SET raw_weight=actual_weight, effective_weight_lb=actual_weight WHERE raw_weight=0 AND actual_weight>0");
    }
    private void createPackingItemsTable(SQLiteDatabase db) { db.execSQL("CREATE TABLE IF NOT EXISTS packing_items (id INTEGER PRIMARY KEY, workout_type TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL)"); }

    private void seedPackingItems(SQLiteDatabase db) {
        seedPackingType(db, "Strength", new String[]{"Athletic shirt", "Shorts", "Workout shoes", "Small sweat towel"});
        seedPackingType(db, "Run", new String[]{"Running shoes", "Shorts", "Athletic shirt", "AirPods / music", "Small sweat towel"});
        seedPackingType(db, "Swim", new String[]{"Swim trunks", "Goggles", "Large towel", "Swim cap", "Pool shoes"});
    }
    private void seedPackingType(SQLiteDatabase db, String type, String[] names) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM packing_items WHERE workout_type=?", new String[]{type})) { if (c.moveToFirst() && c.getInt(0) > 0) return; }
        for (int i=0; i<names.length; i++) { ContentValues v=new ContentValues(); v.put("workout_type",type); v.put("name",names[i]); v.put("position",i); db.insertOrThrow("packing_items",null,v); }

    }

    List<Models.Plan> plans() {
        ArrayList<Models.Plan> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,weekday,type,notes FROM plans WHERE archived=0 ORDER BY name COLLATE NOCASE,id", null)) {
            while (c.moveToNext()) out.add(readPlan(c));
        }
        return out;
    }


    Models.Plan plan(long id) {
        Models.Plan p = null;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,weekday,type,notes FROM plans WHERE id=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) p = readPlan(c);
        }
        if (p == null) return null;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,plan_id,name,position,sets,reps,weight,unit,duration_seconds,distance,notes,rest_seconds,bodyweight,assistance,load_mode,muscle_group FROM exercises WHERE plan_id=? ORDER BY position", new String[]{String.valueOf(id)})) {
            while (c.moveToNext()) p.exercises.add(readExercise(c));
        }
        return p;
    }

    private Models.Plan readPlan(Cursor c) {
        Models.Plan p = new Models.Plan();
        p.id=c.getLong(0); p.name=c.getString(1); p.weekday=c.getInt(2); p.type=c.getString(3); p.notes=c.getString(4);
        return p;
    }

    private Models.Exercise readExercise(Cursor c) {
        Models.Exercise e = new Models.Exercise();
        e.id=c.getLong(0); e.planId=c.getLong(1); e.name=c.getString(2); e.position=c.getInt(3); e.sets=c.getInt(4); e.reps=c.getInt(5); e.weight=c.getDouble(6); e.unit=c.getString(7); e.durationSeconds=c.getInt(8); e.distance=c.getDouble(9); e.notes=c.getString(10); e.restSeconds=c.getInt(11); e.bodyweight=c.getInt(12)!=0; e.assistance=c.getInt(13)!=0; e.loadMode=c.getString(14); e.muscleGroup=c.getString(15);
        if(e.assistance&&e.unit.startsWith("assisted"))e.unit=e.unit.endsWith("kg")?"kg":"lb";
        return e;
    }

    long savePlan(long id, String name, String type, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("type", type); v.put("notes", notes);
        if (id > 0) { getWritableDatabase().update("plans", v, "id=?", new String[]{String.valueOf(id)}); return id; }
        return getWritableDatabase().insertOrThrow("plans", null, v);
    }

    long addExercise(long planId, String name, int sets, int reps, double weight, String unit, String notes, int restSeconds, int durationSeconds, String muscleGroup) {
        int position = 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(position)+1,0) FROM exercises WHERE plan_id=?", new String[]{String.valueOf(planId)})) { if(c.moveToFirst()) position=c.getInt(0); }
        ContentValues v = new ContentValues();
        boolean assistance=unit.startsWith("assisted");
        String storedUnit=assistance?(unit.endsWith("kg")?"kg":"lb"):unit;
        v.put("plan_id",planId); v.put("name",name); v.put("position",position); v.put("sets",sets); v.put("reps",reps); v.put("weight",weight); v.put("unit",storedUnit); v.put("notes",notes); v.put("rest_seconds",restSeconds);
        v.put("bodyweight", unit.equals("bodyweight") ? 1 : 0); v.put("assistance", unit.startsWith("assisted") ? 1 : 0); v.put("load_mode",unit.startsWith("assisted")?WorkoutMath.LOAD_COUNTERBALANCED:unit.equals("bodyweight")?WorkoutMath.LOAD_BODYWEIGHT:WorkoutMath.LOAD_STANDARD); v.put("duration_seconds",durationSeconds); v.put("muscle_group",muscleGroup);
        return getWritableDatabase().insertOrThrow("exercises",null,v);
    }

    private String inferMuscleGroup(String name) {String n=name.toLowerCase(java.util.Locale.ROOT);if(n.contains("leg")||n.contains("calf")||n.contains("squat")||n.contains("hip"))return "Lower body";if(n.contains("ab")||n.contains("plank")||n.contains("back extension"))return "Core";return "Upper body";}

    void deleteExercise(long id) { getWritableDatabase().delete("exercises", "id=?", new String[]{String.valueOf(id)}); }

    void deletePlan(long id) {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues detached=new ContentValues();detached.putNull("plan_id");
            db.update("sessions",detached,"plan_id=?",new String[]{String.valueOf(id)});
            db.delete("exercises","plan_id=?",new String[]{String.valueOf(id)});
            db.delete("plans","id=?",new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    long beginSession(Models.Plan p) {
        ContentValues v = new ContentValues();
        v.put("plan_id",p.id); v.put("plan_name",p.name); v.put("type",p.type); v.put("started_at",java.time.LocalDateTime.now().toString());
        long sessionId=getWritableDatabase().insertOrThrow("sessions", null, v);
        for (Models.Exercise e:p.exercises) addSessionExercise(sessionId,e,e.position);
        return sessionId;
    }

    Models.Profile profile() {
        Models.Profile p=new Models.Profile();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT body_weight,unit FROM profile WHERE id=1",null)) { if(c.moveToFirst()){p.bodyWeight=c.getDouble(0);p.unit=c.getString(1);} }
        return p;
    }
    void saveProfile(double bodyWeight,String unit) { ContentValues v=new ContentValues();v.put("id",1);v.put("body_weight",bodyWeight);v.put("unit",unit);getWritableDatabase().insertWithOnConflict("profile",null,v,SQLiteDatabase.CONFLICT_REPLACE); }

    List<Models.Exercise> sessionExercises(long sessionId) {
        ArrayList<Models.Exercise> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,session_id,name,position,sets,reps,weight,unit,duration_seconds,notes,rest_seconds,bodyweight,load_mode,muscle_group FROM session_exercises WHERE session_id=? ORDER BY position,id",new String[]{String.valueOf(sessionId)})){
            while(c.moveToNext()){Models.Exercise e=new Models.Exercise();e.id=c.getLong(0);e.planId=c.getLong(1);e.name=c.getString(2);e.position=c.getInt(3);e.sets=c.getInt(4);e.reps=c.getInt(5);e.weight=c.getDouble(6);e.unit=c.getString(7);e.durationSeconds=c.getInt(8);e.notes=c.getString(9);e.restSeconds=c.getInt(10);e.bodyweight=c.getInt(11)!=0;e.loadMode=c.getString(12);e.assistance=WorkoutMath.LOAD_COUNTERBALANCED.equals(e.loadMode);e.muscleGroup=c.getString(13);out.add(e);}
        } return out;
    }
    long addSessionExercise(long sessionId, Models.Exercise e, int position) {
        SQLiteDatabase db=getWritableDatabase();db.execSQL("UPDATE session_exercises SET position=position+1 WHERE session_id=? AND position>=?",new Object[]{sessionId,position});
        ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("name",e.name);v.put("position",position);v.put("sets",e.sets);v.put("reps",e.reps);v.put("weight",e.weight);v.put("unit",e.unit);v.put("duration_seconds",e.durationSeconds);v.put("notes",e.notes);v.put("rest_seconds",e.restSeconds);v.put("bodyweight",e.bodyweight?1:0);v.put("load_mode",e.loadMode);v.put("muscle_group",e.muscleGroup);return db.insertOrThrow("session_exercises",null,v);
    }
    void reorderExercises(long planId,List<Models.Exercise> exercises) { SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{for(int i=0;i<exercises.size();i++){ContentValues v=new ContentValues();v.put("position",i);db.update("exercises",v,"id=?",new String[]{String.valueOf(exercises.get(i).id)});}db.setTransactionSuccessful();}finally{db.endTransaction();} }

    void finishSession(long id, String status, String notes) {
        ContentValues v = new ContentValues();
        v.put("ended_at",java.time.LocalDateTime.now().toString()); v.put("status",status); v.put("notes",notes);
        getWritableDatabase().update("sessions",v,"id=?",new String[]{String.valueOf(id)});
    }

    void discardSession(long id) { deleteSession(id); }

    void deleteSession(long id) {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("set_results","session_id=?",new String[]{String.valueOf(id)});
            db.delete("sessions","id=?",new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    void deleteCardio(long id) { getWritableDatabase().delete("cardio","id=?",new String[]{String.valueOf(id)}); }

    void recordSet(long sessionId, Models.Exercise e, int setNumber, int actualReps, double actualWeight, int rir, String status) { recordSet(sessionId,e,setNumber,actualReps,actualWeight,rir,status,0); }
    void recordSet(long sessionId, Models.Exercise e, int setNumber, int actualReps, double actualWeight, int rir, String status, int actualDuration) {
        Models.Profile profile=profile();
        double displayEffective=e.bodyweight?profile.bodyWeight:WorkoutMath.effectiveLoad(e.loadMode,profile.bodyWeight,actualWeight);
        double effectiveLb=WorkoutMath.pounds(displayEffective,e.unit);
        ContentValues v = new ContentValues();
        v.put("session_id",sessionId); v.put("exercise_id",e.id); v.put("exercise_name",e.name); v.put("exercise_position",e.position); v.put("set_number",setNumber); v.put("target_reps",e.reps); v.put("target_weight",e.weight); v.put("target_duration_seconds",e.durationSeconds); v.put("actual_reps",actualReps); v.put("actual_weight",actualWeight); v.put("actual_duration_seconds",actualDuration); v.put("raw_weight",actualWeight);v.put("raw_unit",e.unit);v.put("effective_weight_lb",effectiveLb);v.put("load_mode",e.loadMode);v.put("body_weight_snapshot",profile.bodyWeight); v.put("rir",rir); v.put("status",status); v.put("created_at",java.time.LocalDateTime.now().toString());
        getWritableDatabase().insertOrThrow("set_results",null,v);
    }

    String personalRecord(String exerciseName, int reps, double weight) {
        double heaviest = 0, bestVolume = 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(effective_weight_lb),0),COALESCE(MAX(actual_reps*effective_weight_lb),0) FROM set_results WHERE exercise_name=? AND status='complete'", new String[]{exerciseName})) {
            if (c.moveToFirst()) { heaviest = c.getDouble(0); bestVolume = c.getDouble(1); }
        }
        boolean weightPr = weight > heaviest;
        boolean volumePr = reps * weight > bestVolume;
        if (weightPr && volumePr) return "New weight and set-volume records!";
        if (weightPr) return "New heaviest-weight record!";
        if (volumePr) return "New set-volume record!";
        return null;
    }

    void undoLastSet(long sessionId) {
        getWritableDatabase().execSQL("DELETE FROM set_results WHERE id=(SELECT id FROM set_results WHERE session_id=? ORDER BY id DESC LIMIT 1)",new Object[]{sessionId});
    }

    Models.LastSet lastSet(String exerciseName, int setNumber, long excludingSession) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT actual_reps,actual_weight,rir FROM set_results WHERE exercise_name=? AND set_number=? AND session_id<>? AND status='complete' ORDER BY id DESC LIMIT 1",new String[]{exerciseName,String.valueOf(setNumber),String.valueOf(excludingSession)})) {
            if(c.moveToFirst()) { Models.LastSet s=new Models.LastSet(); s.reps=c.getInt(0);s.weight=c.getDouble(1);s.rir=c.getInt(2);return s; }
        }
        return null;
    }

    List<Models.PackingItem> packingItems(String workoutType) {
        ArrayList<Models.PackingItem> out=new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery("SELECT id,workout_type,name,position FROM packing_items WHERE workout_type=? ORDER BY position,id",new String[]{workoutType})) {
            while(c.moveToNext()){Models.PackingItem item=new Models.PackingItem();item.id=c.getLong(0);item.workoutType=c.getString(1);item.name=c.getString(2);item.position=c.getInt(3);out.add(item);}
        } return out;
    }
    void addPackingItem(String workoutType,String name) {
        int position=0;try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(position)+1,0) FROM packing_items WHERE workout_type=?",new String[]{workoutType})){if(c.moveToFirst())position=c.getInt(0);}
        ContentValues v=new ContentValues();v.put("workout_type",workoutType);v.put("name",name);v.put("position",position);getWritableDatabase().insertOrThrow("packing_items",null,v);
    }
    void deletePackingItem(long id){getWritableDatabase().delete("packing_items","id=?",new String[]{String.valueOf(id)});}

    int completedSetCount(long sessionId) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM set_results WHERE session_id=?",new String[]{String.valueOf(sessionId)})){return c.moveToFirst()?c.getInt(0):0;}
    }

    void addCardio(String activity, String date, double duration, double distance, String unit, String intervals, String notes, int laps, double poolLength) {
        ContentValues v=new ContentValues(); v.put("activity",activity);v.put("date",date);v.put("duration_min",duration);v.put("distance",distance);v.put("unit",unit);v.put("intervals",intervals);v.put("notes",notes);v.put("laps",laps);v.put("pool_length",poolLength);
        getWritableDatabase().insertOrThrow("cardio",null,v);
    }
}
