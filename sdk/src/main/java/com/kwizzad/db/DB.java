package com.kwizzad.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.kwizzad.log.QLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.functions.Action1;

/**
 * simplified db storage solution with direct storage, no offloading
 * will not work for every use case but should be perfectly fine here
 */
@SuppressWarnings("unchecked")
public class DB {
    public static final DB instance = new DB();

    private Context context;
    private DBOpenHelper dbHelper;

    // TODO: unify
    private final Queue<Initme> queue = new ConcurrentLinkedQueue<>();
    private final Queue<InitList> listqueue = new ConcurrentLinkedQueue<>();

    private static final class Initme {
        public final String key;
        public final Class clazz;
        public final Object defaultValue;
        // TODO: weak reference
        public final Action1 initFunc;
        public final String password;

        public Initme(String key, Class clazz, Object defaultValue, Action1 initFunc, String password) {
            this.key = key;
            this.clazz = clazz;
            this.defaultValue = defaultValue;
            this.initFunc = initFunc;
            this.password = password;
        }

    }

    private static final class InitList {
        public final String key;
        public final Class clazz;
        public final Action1 initFunc;
        public final String password;

        public InitList(String key, Class clazz, Action1 initFunc, String password) {
            this.key = key;
            this.clazz = clazz;
            this.initFunc = initFunc;
            this.password = password;
        }

    }

    private DB() {
    }

    public void init(Context context) {
        if (this.context == null || this.context != context.getApplicationContext()) {

            QLog.d("initializing DB");

            this.context = context.getApplicationContext();

            dbHelper = new DBOpenHelper(this.context);

            while (!queue.isEmpty()) {
                final Initme im = queue.poll();
                if (im != null) {
                    rinit(im);
                }
            }

            while (!listqueue.isEmpty()) {
                final InitList im = listqueue.poll();
                if (im != null) {
                    rinit(im);
                }
            }

        } else {
            QLog.d("db already initialized");

        }
    }

    public <T> void requestInit(String key, Class<? super T> clazz, T defaultValue, Action1<? super T> initFunc, String password) {
        Initme im = new Initme(key, clazz, defaultValue, initFunc, password);
        if (context != null) {
            rinit(im);
        } else {
            queue.add(im);
        }
    }

    public <T> void requestListInit(String key, Class<? super T> clazz, Action1<List<T>> initFunc, String password) {
        InitList im = new InitList(key, clazz, initFunc, password);
        if (context != null) {
            rinit(im);
        } else {
            listqueue.add(im);
        }
    }

    @SuppressWarnings("unchecked")
    private void rinit(Initme im) {
        im.initFunc.call(_get(im.key, im.clazz, im.defaultValue, im.password));
    }

    @SuppressWarnings("unchecked")
    private void rinit(InitList im) {
        im.initFunc.call(_getList(im.key, im.clazz, im.password));
    }

    public void store(final String key, final Object data, String password) {
        if (context == null) {
            QLog.e("DB was not initialized yet");
            return;
        }

        try {

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (db != null) {
                _store(db, key, data, password);
            } else {
                QLog.e("DB IS NULL");
            }

        } catch (Exception e) {
            e.printStackTrace();

            try {
                dbHelper.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }

            try {

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    _store(db, key, data, password);
                } else {
                    QLog.e("DB IS NULL");
                }

            } catch (Exception ee) {
                ee.printStackTrace();
                QLog.e("---- FATAL ---- could not save: " + key);
            }

        }

    }

    private void _store(SQLiteDatabase db, String key, Object data, String password) {
        try {
            ContentValues values = new ContentValues();

            values.put(DBOpenHelper.COLUMN_ID, key);

            String dval = convertToStorage(data);

            QLog.d("saving " + key + ": " + dval);

            if (dval != null) {
                if (password != null)
                    dval = AES.encrypt(password, dval);

                values.put(DBOpenHelper.COLUMN_VALUE, dval);
            } else {
                values.putNull(DBOpenHelper.COLUMN_VALUE);
            }


            db.replace(DBOpenHelper.TABLE_NAME, null, values);


        } catch (Exception e) {
            QLog.e(e);
        }
    }

    public static String convertToStorage(Object data) {
        /*if (data instanceof IJson) {
            try {
                JSONObject o = new JSONObject();
                if (data != null && ((IJson) data).toJson(o)) {
                    return o.toString();
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        else */
        if (data instanceof Collection) {
            JSONArray arr = new JSONArray();
            for (Object el : ((Collection) data)) {
                arr.put(convertToStorage(el));
            }
            return arr.toString();
        } else if (data != null) {

            if (data instanceof String) {
                return data.toString();
            }

            try {
                Class type = data.getClass();
                final List<Method> methods = getMethodsAnnotatedWith(type, ToJson.class);
                for (Method m : methods) {
                    if (!Modifier.isStatic(m.getModifiers())) {
                        if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isAssignableFrom(JSONObject.class)) {
                            JSONObject o = new JSONObject();
                            m.invoke(data, o);
                            return o.toString();
                        }
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return null;
            }

            // fallback
            return data.toString();
        }
        return null;
    }

    public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<>();
        for (final Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                methods.add(method);
            }
        }
        return methods;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, T defaultValue, String password) {
        return (T) _get(key, type, defaultValue, password);
    }


    private Object _get(String key, Class type, Object defaultValue, String password) {


        try {

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (db != null) {
                return _get(db, key, type, defaultValue, password);
            } else {
                QLog.e("DB IS NULL");
            }

        } catch (Exception e) {
            QLog.e(e);

            try {
                dbHelper.close();
            } catch (Exception ee) {
                QLog.e(ee);
            }

            try {

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db != null) {
                    return _get(db, key, type, defaultValue, password);
                } else {
                    QLog.e("DB IS NULL");
                }

            } catch (Exception ee) {
                QLog.e(ee, "---- FATAL ---- could not read: " + key);
            }

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Object _get(SQLiteDatabase db, String key, Class type, Object defaultValue, String password) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + DBOpenHelper.COLUMN_VALUE + " FROM " + DBOpenHelper.TABLE_NAME + " WHERE " + DBOpenHelper.COLUMN_ID + "=?", new String[]{key});

            if (cursor.moveToFirst()) {
                String dbdata = cursor.getString(0);

                if (dbdata != null && dbdata.length() > 0) {

                    if (password != null)
                        dbdata = AES.decrypt(password, dbdata);

                    return convertFromStorage(dbdata, type);
                } else {
                    return defaultValue;
                }
            }

            cursor.close();
        } catch (Exception e) {
            QLog.d(e);
        } finally {
            try {
                if (cursor != null)
                    cursor.close();
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    public static Object convertFromStorage(String dbdata, Class type) {

        if (type.equals(String.class)) {
            return dbdata;
        }

        try {
            try {

                for (Method m : getMethodsAnnotatedWith(type, FromJson.class)) {
                    if (
                            m.getParameterTypes().length == 1
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes()[0].isAssignableFrom(JSONObject.class)
                            ) {


                        QLog.d("trying requestMethod " + m + "\nwith " + dbdata);

                        JSONObject jsonObject = new JSONObject(dbdata);

                        Object o = type.newInstance();

                        return m.invoke(o, jsonObject);


                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }

            try {
                Constructor c = type.getConstructor(String.class);
                return c.newInstance(dbdata);
            } catch (Exception e) {

                try {
                    return Enum.valueOf(type, dbdata);
                } catch (Exception ignored) {
                }

                for (Method m : type.getMethods()) {
                    if (
                            m.getParameterTypes().length == 1
                                    && Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes()[0].isAssignableFrom(String.class)
                                    && type.isAssignableFrom(m.getReturnType())
                            ) {

                        try {
                            //QLog.d("trying requestMethod " + m + " with " + json);

                            return m.invoke(null, dbdata);
                        } catch (Exception ignored) {
                        }

                    }
                }

            }

        } catch (Exception e) {
            QLog.d(e);
        }
        return null;
    }

    public <T> List<T> getList(String key, Class<T> type, String password) {
        return (List<T>) _getList(key, type, password);
    }

    private List _getList(String key, Class type, String password) {
        try {

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            if (db != null) {
                return _getList(db, key, type, password);
            } else {
                QLog.e("DB IS NULL");
            }

        } catch (Exception e) {
            QLog.e(e);

            try {
                dbHelper.close();
            } catch (Exception ee) {
                QLog.e(ee);
            }

            try {

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                if (db != null) {
                    return _getList(db, key, type, password);
                } else {
                    QLog.e("DB IS NULL");
                }

            } catch (Exception ee) {
                QLog.e(ee, "---- FATAL ---- could not read: " + key);
            }

        }

        return new ArrayList<>();
    }

    // TODO: unify
    @SuppressWarnings("unchecked")
    private List _getList(SQLiteDatabase db, String key, Class type, String password) {
        List out = new ArrayList();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + DBOpenHelper.COLUMN_VALUE + " FROM " + DBOpenHelper.TABLE_NAME + " WHERE " + DBOpenHelper.COLUMN_ID + "=?", new String[]{key});

            if (cursor.moveToFirst()) {

                String s = cursor.getString(0);

                if (s == null || s.length() == 0)
                    return out;

                if (password != null)
                    s = AES.decrypt(password, s);

                JSONArray json = new JSONArray(s);

                for (int ii = 0; ii < json.length(); ii++) {
                    Object o = json.get(ii);

                    if (o instanceof String) {
                        try {
                            out.add(Enum.valueOf(type, (String) o));
                        } catch (Exception ignored) {
                        }
                    }
                }

            }

            cursor.close();
        } catch (Exception e) {
            QLog.e(e);
        } finally {
            try {
                if (cursor != null)
                    cursor.close();
            } catch (Exception ignored) {
            }
        }
        QLog.d(key + " can not be read from db");
        return out;
    }
}
