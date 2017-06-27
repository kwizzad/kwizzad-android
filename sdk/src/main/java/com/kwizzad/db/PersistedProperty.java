package com.kwizzad.db;

import com.kwizzad.log.QLog;
import com.kwizzad.property.Property;

/**
 * This is an automatically persisted Property, that will save and load data to the
 * {@link DB#instance}.
 * Be aware that the Data will only be loaded after the DB has been initialized, which
 * might be later than your Property has been initialized. Therefore you have to make sure
 * to initialize the DB as early as possible in your Application.
 * This is a simplified behaviour, but it saves a lot of crazy complexity.
 *
 * @param <T> the type of the property
 */
public class PersistedProperty<T> extends Property<T> {

    private final String key;
    private final Class<T> type;
    private final String pw;

    /**
     * Create a new Property with a given key and a type and no default value
     *
     * @param key  The database key
     * @param type The type of the Property
     */
    public PersistedProperty(String key, Class<T> type) {
        this(key, type, null);
    }

    public PersistedProperty(String key, Class<T> type, String pw) {
        super(null);
        this.key = key;
        this.type = type;
        this.pw = pw;
        init();
    }

    /**
     * Create a new Property with a given key and a default Value.
     * This value can be overridden by a current value in the db, if there is one.
     * Be aware, that it can still be null, when it has been explicitly set to null in the database.
     *
     * @param key          The database key
     * @param defaultValue A default value to apply, when there is no database key.
     */
    public PersistedProperty(String key, T defaultValue) {
        this(key, defaultValue, null);
    }

    @SuppressWarnings("unchecked")
    public PersistedProperty(String key, T defaultValue, String pw) {
        super(defaultValue);
        this.key = key;
        this.type = (Class<T>) defaultValue.getClass();
        this.pw = pw;
        init();
    }

    private void init() {
        DB.instance.requestInit(key, type, value, dbValue -> {
            boolean changed = false;
            QLog.d("PersistedProperty initialized '" + key + "' = " + dbValue);
            if (dbValue != value) {

                changed = true;
            }
            value = dbValue;
            if (changed) {
                notifyChanged();
            }
        }, pw);
    }

    @Override
    public void set(T value) {
        QLog.d("PersistedProperty saving '" + key + "' = " + value);
        DB.instance.store(key, value, pw);
        super.set(value);
    }

    @Override
    public String toString() {
        return "PersistedProperty '" + key + "' = " + value;
    }

    /**
     * create a new Property with a given key and a type. No default value.
     *
     * @param key   The database key
     * @param clazz The Class type
     * @param <T>   The type
     * @return A new Persisted Property
     */
    public static <T> PersistedProperty<T> create(String key, Class<T> clazz) {
        return new PersistedProperty<>(key, clazz);
    }

    public static <T> PersistedProperty<T> create(String key, Class<T> clazz, String pw) {
        return new PersistedProperty<>(key, clazz, pw);
    }
}