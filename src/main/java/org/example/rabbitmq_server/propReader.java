package org.example.rabbitmq_server;

import java.io.InputStream;
import java.util.Properties;

public class propReader {
    private Properties prop;
    public propReader() {
        loadProp();
    }

    private void loadProp(){
        prop = new Properties();
        try(InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")){

            if(input == null){
                System.out.println("Properties dosyası bulunamadı");
                return;
            }
            prop.load(input);
        }catch (Exception e){
            System.out.println("Properties dosyası okunamadı: " + e.getMessage());
        }
    }

    public String getValue(String key){
        if (prop == null) {
            return null;
        }
        return prop.getProperty(key);
    }
    
    public String getValue(String key, String defaultValue){
        if (prop == null) {
            return defaultValue;
        }
        return prop.getProperty(key, defaultValue);
    }
}
