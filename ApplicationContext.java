package org.lmd.config;

import org.lmd.annotations.Autowired;
import org.lmd.annotations.Component;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ApplicationContext {
    private static Map<Class, Object> beans = new HashMap<>();
    private static String rootPath;

    public ApplicationContext(String basePackage) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        basePackage = basePackage.replaceAll("\\.", "/");
        rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        //扫描包的绝对路径
        String path= rootPath+basePackage;
        File file = new File(path);
        //添加bean实例
        load(file);
        //DI
        load();
    }

    private void load(File file) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //文件目录,递归遍历
        if (file.isDirectory()) {
            for (File listFile : file.listFiles()) {
                load(listFile);
            }
            return;
        }
        //非class字节码文件,不做处理
        if (!file.getName().endsWith(".class")) return;
        //获取类的全路径,便于进行反射
        String relativePath = file.getAbsolutePath().substring(rootPath.length()-1)
                .replaceAll("\\\\","\\.").replaceAll("\\.class","");
        Class clazz = Class.forName(relativePath);
        Annotation annotationsComponent = clazz.getAnnotation(Component.class);
        if(null==annotationsComponent) return;
        Constructor declaredConstructor = clazz.getDeclaredConstructor();
        declaredConstructor.setAccessible(true);
        Object o =declaredConstructor.newInstance();
        Class[] interfaces = clazz.getInterfaces();
        beans.put(interfaces.length==0?clazz:interfaces[0],o);
    }

    private void load() throws IllegalAccessException {
        Set<Map.Entry<Class, Object>> entries = beans.entrySet();
        for (Map.Entry<Class, Object> entry : entries) {
            Object value = entry.getValue();
            for (Field declaredField : value.getClass().getDeclaredFields()) {
                Autowired annotation = declaredField.getAnnotation(Autowired.class);
                if(null!=annotation){
                    declaredField.setAccessible(true);
                    declaredField.set(value,beans.get(declaredField.getType()));
                }
            }
        }
    }
    public Object getBean(Class clazz){
        return beans.get(clazz);
    }
}
