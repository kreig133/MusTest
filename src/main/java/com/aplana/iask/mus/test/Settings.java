package com.aplana.iask.mus.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
public class Settings {

    public static final String DRIVER = "db.driver";
    public static final String URL = "db.url";
    public static final String USERNAME = "db.username";
    public static final String PASSWORD = "db.password";
    public static final String IASK_URL = "iask.url";
    public static final String DRIVER_URL = "driver.url";

    public static final String PROPS = "app.properties";

    private static final Properties properties = new Properties();

    static {
        try {
            FileInputStream props = new FileInputStream(PROPS);
            properties.load(props);
            props.close();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить параметры программы", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static Boolean getBoolean(String key) {
        return "true".equals(get(key));
    }

}
