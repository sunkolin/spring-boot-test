package com.starfish.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

public class Main {

    private static final ObjectMapper j2Mapper = new ObjectMapper();
    private static final tools.jackson.databind.ObjectMapper j3Mapper = new tools.jackson.databind.ObjectMapper();

    public static void main(String[] args) {
        scanAndTestJackson();
    }

    private static void scanAndTestJackson() {
        String basePackage = "com.starfish.test";
        Set<Class<?>> controllerClasses = scanControllerClasses(basePackage);

        if (controllerClasses.isEmpty()) {
            System.out.println("未找到任何 Controller 类");
            return;
        }

        Set<Type> paramTypes = new LinkedHashSet<>();
        Set<Type> returnTypes = new LinkedHashSet<>();

        for (Class<?> controller : controllerClasses) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isMappingMethod(method)) continue;

                for (Class<?> paramType : method.getParameterTypes()) {
                    if (!isBasicOrCommonType(paramType)) {
                        paramTypes.add(paramType);
                    }
                }

                Type returnType = method.getGenericReturnType();
                if (returnType instanceof ParameterizedType pt) {
                    for (Type arg : pt.getActualTypeArguments()) {
                        if (arg instanceof Class<?> cls && !isBasicOrCommonType(cls)) {
                            returnTypes.add(cls);
                        }
                    }
                } else if (returnType instanceof Class<?> cls && !isBasicOrCommonType(cls)) {
                    returnTypes.add(cls);
                }
            }
        }

        System.out.println("========== 扫描结果 ==========");
        System.out.println("找到 " + controllerClasses.size() + " 个 Controller");
        System.out.println("入参类型: " + paramTypes.size() + " 种");
        System.out.println("出参类型: " + returnTypes.size() + " 种");

        System.out.println("\n========== 入参类型 Jackson2 vs Jackson3 ==========");
        for (Type type : paramTypes) {
            testType(type);
        }

        System.out.println("\n========== 出参类型 Jackson2 vs Jackson3 ==========");
        for (Type type : returnTypes) {
            testType(type);
        }
    }

    private static boolean isMappingMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, GetMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PostMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PutMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PatchMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, RequestMapping.class) != null;
    }

    private static boolean isBasicOrCommonType(Type type) {
        if (!(type instanceof Class<?> cls)) return false;
        return cls.isPrimitive() ||
               cls == String.class ||
               Number.class.isAssignableFrom(cls) ||
               cls == Boolean.class ||
               cls == Character.class ||
               cls.getName().startsWith("java.") ||
               cls.getName().startsWith("jakarta.") ||
               cls.getName().startsWith("javax.") ||
               cls.getName().startsWith("org.springframework") ||
               cls.getName().startsWith("com.baomidou") ||
               cls.getName().startsWith("com.github.sunkolin.starfish.core.model") ||
               Iterable.class.isAssignableFrom(cls) ||
               Map.class.isAssignableFrom(cls);
    }

    private static Set<Class<?>> scanControllerClasses(String basePackage) {
        Set<Class<?>> controllers = new LinkedHashSet<>();
        String packagePath = basePackage.replace('.', '/');

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                findControllers(directory, basePackage, controllers);
            }
        } catch (Exception e) {
            System.out.println("扫描类失败: " + e.getMessage());
        }

        return controllers;
    }

    private static void findControllers(File directory, String packageName, Set<Class<?>> controllers) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findControllers(file, packageName + "." + file.getName(), controllers);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                try {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (AnnotationUtils.findAnnotation(clazz, RestController.class) != null) {
                        controllers.add(clazz);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void testType(Type type) {
        if (!(type instanceof Class<?> clazz)) {
            System.out.println("跳过非Class类型: " + type);
            return;
        }

        try {
            Object instance = createInstance(clazz);
            if (instance == null) {
                System.out.println("跳过无法实例化的类型: " + clazz.getSimpleName());
                return;
            }

            String json2 = j2Mapper.writeValueAsString(instance);
            String json3 = j3Mapper.writeValueAsString(instance);

            Set<String> keys2 = extractKeys(json2);
            Set<String> keys3 = extractKeys(json3);

            boolean same = keys2.equals(keys3);

            System.out.println("\n【" + clazz.getSimpleName() + "】");
            System.out.println("  Jackson2 keys: " + sortAndJoin(keys2));
            System.out.println("  Jackson3 keys: " + sortAndJoin(keys3));

            if (same) {
                System.out.println("  ✅ key 一致");
            } else {
                Set<String> onlyInJ2 = new HashSet<>(keys2);
                onlyInJ2.removeAll(keys3);
                Set<String> onlyInJ3 = new HashSet<>(keys3);
                onlyInJ3.removeAll(keys2);
                System.out.println("  ❌ key 不一致");
                if (!onlyInJ2.isEmpty()) System.out.println("  Jackson2 独有: " + sortAndJoin(onlyInJ2));
                if (!onlyInJ3.isEmpty()) System.out.println("  Jackson3 独有: " + sortAndJoin(onlyInJ3));
            }
        } catch (Exception e) {
            System.out.println("\n【" + clazz.getSimpleName() + "】序列化失败: " + e.getMessage());
        }
    }

    private static Object createInstance(Class<?> clazz) {
        try {
            java.lang.reflect.Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> extractKeys(String json) {
        Set<String> keys = new LinkedHashSet<>();
        if (json == null || json.length() < 2) return keys;
        extractKeysRecursive(json, keys, 0);
        return keys;
    }

    private static void extractKeysRecursive(String json, Set<String> keys, int start) {
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                int end = json.indexOf('"', i + 1);
                if (end > 0 && end < json.length()) {
                    int colonIdx = json.indexOf(':', end + 1);
                    int nextQuote = json.indexOf('"', end + 1);
                    if (colonIdx > 0 && (nextQuote < 0 || colonIdx < nextQuote)) {
                        String key = json.substring(i + 1, end);
                        if (nextQuote > 0 && nextQuote < json.length() && end + 1 < json.length() && json.charAt(end + 1) != ':') {
                            i = nextQuote + 1;
                            continue;
                        }
                        keys.add(key);
                    }
                }
            } else if (c == '{') {
                extractKeysRecursive(json, keys, i + 1);
            }
            i++;
        }
    }

    private static String sortAndJoin(Set<String> keys) {
        List<String> sorted = new ArrayList<>(keys);
        Collections.sort(sorted);
        return String.join(", ", sorted);
    }
}