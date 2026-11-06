package com.starfish.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.*;

public class JacksonCompare {

    /**
     * Bean所在包，如果项目中有特殊包，请在这里加 TODO
     */
    private static final List<String> CONTROLLER_PACKAGE_LIST = Arrays.asList(
            ".controller",
            ".web"
    );

    /**
     * Bean所在包，如果项目中有特殊包，请在这里加 TODO
     */
    private static final List<String> BEAN_PACKAGE_LIST = Arrays.asList(
            ".entity",
            ".model",
            ".vo",
            ".param",
            ".query",
            ".res",
            ".req",
            ".result",
            ".request",
            ".response",
            ".domain",
            ".do",
            ".dto",
            ".po",
            ".bo",
            ".pojo"
    );

    private static final String BASE_PACKAGE = JacksonCompare.class.getPackage().getName();

    private static final ObjectMapper j2Mapper = new ObjectMapper();

    private static final tools.jackson.databind.ObjectMapper j3Mapper = new tools.jackson.databind.ObjectMapper();

    public static void main(String[] args) {
        Set<Class<?>> controllerTypes = scanAndTestJackson();
        scanUnreferencedBeans(controllerTypes);
    }

    private static Set<Class<?>> scanAndTestJackson() {

        Set<Class<?>> controllerClasses = scanControllerClasses(BASE_PACKAGE);

        if (controllerClasses.isEmpty()) {
            System.out.println("未找到任何 Controller 类");
            return Collections.emptySet();
        }

        Map<Type, Set<String>> typeSources = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        for (Class<?> controller : controllerClasses) {
            String baseUrl = getRequestMappingBase(controller);
            String controllerName = controller.getSimpleName();

            for (Method method : controller.getDeclaredMethods()) {
                if (!isMappingMethod(method)) continue;

                String methodUrl = getMappingUrl(method);
                String fullUrl = (baseUrl + methodUrl).replace("//", "/");
                String source = controllerName + "#" + method.getName() + " -> " + fullUrl;

                for (java.lang.reflect.Type genericParamType : method.getGenericParameterTypes()) {
                    collectTypes(genericParamType, source, typeSources);
                }

                Type returnType = method.getGenericReturnType();
                collectTypes(returnType, source, typeSources);

                checkAmbiguousReturnType(returnType, source, warnings);
            }
        }

        System.out.println("========== 扫描结果 ==========");
        System.out.println("找到 " + controllerClasses.size() + " 个 Controller");
        System.out.println("入参+出参类型: " + typeSources.size() + " 种");

        if (!warnings.isEmpty()) {
            System.out.println("\n⚠️  发现 " + warnings.size() + " 处返回类型不明确，无法扫描到实际业务模型:");
            for (String w : warnings) {
                System.out.println("  ⚠️  " + w);
            }
            System.out.println("  💡 建议将 Result<?> / Result<Object> 改为具体类型，如 Result<UserEntity>");
        }

        System.out.println("\n========== Jackson2 vs Jackson3 key 对比 ==========");
        boolean first = true;
        for (Map.Entry<Type, Set<String>> entry : typeSources.entrySet()) {
            if (!first) System.out.println();
            first = false;
            System.out.println("  来源接口: " + String.join(" | ", entry.getValue()));
            testType(entry.getKey());
        }

        Set<Class<?>> result = new LinkedHashSet<>();
        for (Type t : typeSources.keySet()) {
            if (t instanceof Class<?> cls) result.add(cls);
        }
        return result;
    }

    private static boolean isMappingMethod(Method method) {
        return AnnotationUtils.findAnnotation(method, GetMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PostMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PutMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, DeleteMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, PatchMapping.class) != null ||
               AnnotationUtils.findAnnotation(method, RequestMapping.class) != null;
    }

    private static String getRequestMappingBase(Class<?> controller) {
        RequestMapping rm = AnnotationUtils.findAnnotation(controller, RequestMapping.class);
        if (rm != null && rm.value().length > 0) {
            return rm.value()[0];
        }
        return "";
    }

    private static String getMappingUrl(Method method) {
        GetMapping gm = AnnotationUtils.findAnnotation(method, GetMapping.class);
        if (gm != null && gm.value().length > 0) return gm.value()[0];
        PostMapping pm = AnnotationUtils.findAnnotation(method, PostMapping.class);
        if (pm != null && pm.value().length > 0) return pm.value()[0];
        PutMapping pum = AnnotationUtils.findAnnotation(method, PutMapping.class);
        if (pum != null && pum.value().length > 0) return pum.value()[0];
        DeleteMapping dm = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
        if (dm != null && dm.value().length > 0) return dm.value()[0];
        PatchMapping pam = AnnotationUtils.findAnnotation(method, PatchMapping.class);
        if (pam != null && pam.value().length > 0) return pam.value()[0];
        RequestMapping rm = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (rm != null && rm.value().length > 0) return rm.value()[0];
        return "";
    }

    private static void collectTypes(Type type, String source, Map<Type, Set<String>> typeSources) {
        if (type instanceof ParameterizedType pt) {
            for (Type arg : pt.getActualTypeArguments()) {
                collectTypes(arg, source, typeSources);
            }
        } else if (type instanceof Class<?> cls && !isBasicOrCommonType(cls)) {
            typeSources.computeIfAbsent(cls, k -> new LinkedHashSet<>()).add(source);
        }
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
               Iterable.class.isAssignableFrom(cls) ||
               Map.class.isAssignableFrom(cls);
    }

    private static void checkAmbiguousReturnType(Type returnType, String source, List<String> warnings) {
        if (!(returnType instanceof ParameterizedType pt)) return;

        for (Type arg : pt.getActualTypeArguments()) {
            if (arg instanceof WildcardType) {
                warnings.add(source + " 返回类型: " + returnType + " (通配符 ? 无法推断)");
                return;
            }
            if (arg instanceof Class<?> cls && cls == Object.class) {
                warnings.add(source + " 返回类型: " + returnType + " (Object 无法推断)");
                return;
            }
            if (arg instanceof ParameterizedType nested) {
                checkAmbiguousReturnType(nested, source, warnings);
            }
        }
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

        boolean matchPackage = isControllerPackage(packageName);

        for (File file : files) {
            if (file.isDirectory()) {
                findControllers(file, packageName + "." + file.getName(), controllers);
            } else if (matchPackage && file.getName().endsWith(".class") && !file.getName().contains("$")) {
                try {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (AnnotationUtils.findAnnotation(clazz, Controller.class) != null) {
                        controllers.add(clazz);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean isControllerPackage(String packageName) {
        for (String suffix : CONTROLLER_PACKAGE_LIST) {
            if (packageName.contains(suffix)) return true;
        }
        return false;
    }

    private static void scanUnreferencedBeans(Set<Class<?>> controllerTypes) {
        Set<Class<?>> allBeanClasses = scanBeanClasses(BASE_PACKAGE);

        if (allBeanClasses.isEmpty()) {
            System.out.println("\n========== 未使用的 Bean 对比 ==========");
            System.out.println("未找到任何 Bean 类（BEAN_PACKAGE_LIST: " + BEAN_PACKAGE_LIST + "）");
            return;
        }

        Set<Class<?>> unreferenced = new LinkedHashSet<>(allBeanClasses);
        unreferenced.removeAll(controllerTypes);

        System.out.println("\n========== 未使用的 Bean 对比 ==========");
        System.out.println("Bean 包下共 " + allBeanClasses.size() + " 个类");
        System.out.println("Controller 中已引用: " + (allBeanClasses.size() - unreferenced.size()) + " 个");
        System.out.println("未被 Controller 引用: " + unreferenced.size() + " 个");

        if (unreferenced.isEmpty()) {
            System.out.println("✅ 所有 Bean 类都已在 Controller 中使用");
            return;
        }

        System.out.println("\n未被引用的 Bean 类 Jackson2 vs Jackson3 key 对比:");
        boolean first = true;
        int inconsistentCount = 0;
        for (Class<?> cls : unreferenced) {
            if (!first) System.out.println();
            first = false;
            boolean consistent = isJacksonConsistent(cls);
            if (!consistent) inconsistentCount++;
            testType(cls);
        }

        if (inconsistentCount > 0) {
            System.out.println("\n⚠️  其中 " + inconsistentCount + " 个未被引用的 Bean 在 Jackson2/Jackson3 下 key 不一致");
            System.out.println("  💡 这些类不在 Controller 入参/出参中，但建议手动确认字段 key 是否符合预期");
        } else {
            System.out.println("\n✅ 所有未被引用的 Bean 在 Jackson2/Jackson3 下 key 一致");
        }
    }

    private static Set<Class<?>> scanBeanClasses(String basePackage) {
        Set<Class<?>> beans = new LinkedHashSet<>();
        String packagePath = basePackage.replace('.', '/');

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                findBeanClasses(directory, basePackage, beans);
            }
        } catch (Exception ignored) {
        }

        return beans;
    }

    private static void findBeanClasses(File directory, String packageName, Set<Class<?>> beans) {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        boolean matchPackage = isBeanPackage(packageName);

        for (File file : files) {
            if (file.isDirectory()) {
                findBeanClasses(file, packageName + "." + file.getName(), beans);
            } else if (matchPackage && file.getName().endsWith(".class") && !file.getName().contains("$")) {
                try {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (!isBasicOrCommonType(clazz) && !clazz.isInterface() && !clazz.isEnum()) {
                        beans.add(clazz);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean isBeanPackage(String packageName) {
        for (String suffix : BEAN_PACKAGE_LIST) {
            if (packageName.contains(suffix)) return true;
        }
        return false;
    }

    private static boolean isJacksonConsistent(Class<?> clazz) {
        try {
            Object instance = createInstance(clazz);
            if (instance == null) return true;
            String json2 = j2Mapper.writeValueAsString(instance);
            String json3 = j3Mapper.writeValueAsString(instance);
            return extractKeys(json2).equals(extractKeys(json3));
        } catch (Exception e) {
            return true;
        }
    }

    private static void printJacksonDiff(Class<?> clazz) {
        try {
            Object instance = createInstance(clazz);
            if (instance == null) {
                System.out.println("    无法实例化，跳过详细对比");
                return;
            }
            String json2 = j2Mapper.writeValueAsString(instance);
            String json3 = j3Mapper.writeValueAsString(instance);
            Set<String> keys2 = extractKeys(json2);
            Set<String> keys3 = extractKeys(json3);
            Set<String> onlyInJ2 = new HashSet<>(keys2);
            onlyInJ2.removeAll(keys3);
            Set<String> onlyInJ3 = new HashSet<>(keys3);
            onlyInJ3.removeAll(keys2);
            System.out.println("    Jackson2 keys: " + sortAndJoin(keys2));
            System.out.println("    Jackson3 keys: " + sortAndJoin(keys3));
            if (!onlyInJ2.isEmpty()) System.out.println("    Jackson2 独有: " + sortAndJoin(onlyInJ2));
            if (!onlyInJ3.isEmpty()) System.out.println("    Jackson3 独有: " + sortAndJoin(onlyInJ3));
        } catch (Exception e) {
            System.out.println("    序列化失败: " + e.getMessage());
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

            System.out.println("【" + clazz.getSimpleName() + "】");
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