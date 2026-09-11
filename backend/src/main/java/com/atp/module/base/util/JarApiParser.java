package com.atp.module.base.util;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Jar 包接口解析器：解析 Spring MVC 注解，提取接口的请求方式与真实路径。
 */
public class JarApiParser {

    private static final String REST_CONTROLLER = "Lorg/springframework/web/bind/annotation/RestController;";
    private static final String CONTROLLER = "Lorg/springframework/web/bind/annotation/Controller;";
    private static final String REQUEST_MAPPING = "Lorg/springframework/web/bind/annotation/RequestMapping;";
    private static final String GET_MAPPING = "Lorg/springframework/web/bind/annotation/GetMapping;";
    private static final String POST_MAPPING = "Lorg/springframework/web/bind/annotation/PostMapping;";
    private static final String PUT_MAPPING = "Lorg/springframework/web/bind/annotation/PutMapping;";
    private static final String DELETE_MAPPING = "Lorg/springframework/web/bind/annotation/DeleteMapping;";
    private static final String PATCH_MAPPING = "Lorg/springframework/web/bind/annotation/PatchMapping;";

    /** 解析结果：接口名称、请求方式、请求路径 */
    public record ParsedApi(String name, String method, String path) {
    }

    public List<ParsedApi> parse(InputStream jarInputStream) throws IOException {
        List<ParsedApi> result = new ArrayList<>();
        try (JarInputStream jar = new JarInputStream(jarInputStream)) {
            JarEntry entry;
            while ((entry = jar.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                if (!entryName.endsWith(".class") || entryName.endsWith("module-info.class")) {
                    continue;
                }
                byte[] bytes = jar.readAllBytes();
                try {
                    ClassReader reader = new ClassReader(bytes);
                    ControllerClassVisitor visitor = new ControllerClassVisitor(simpleClassName(entryName));
                    reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    result.addAll(visitor.getApis());
                } catch (Exception ignored) {
                    // 跳过无法解析的类
                }
            }
        }
        return result;
    }

    private static String simpleClassName(String entryName) {
        String name = entryName.substring(0, entryName.length() - ".class".length());
        int slash = name.lastIndexOf('/');
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    static String combinePath(String classPath, String methodPath) {
        String base = classPath == null ? "" : classPath;
        String sub = methodPath == null ? "" : methodPath;
        String joined;
        if (base.isEmpty()) {
            joined = sub;
        } else if (sub.isEmpty()) {
            joined = base;
        } else {
            joined = base + "/" + sub;
        }
        String normalized = joined.replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** 解析类级注解，记录 Controller 标记与基础路径 */
    private static class ControllerClassVisitor extends ClassVisitor {
        private final String className;
        private boolean isController;
        private final List<String> classPaths = new ArrayList<>();
        private final List<ParsedApi> apis = new ArrayList<>();

        ControllerClassVisitor(String className) {
            super(Opcodes.ASM9);
            this.className = className;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (REST_CONTROLLER.equals(descriptor) || CONTROLLER.equals(descriptor)) {
                isController = true;
                return null;
            }
            if (REQUEST_MAPPING.equals(descriptor)) {
                return new PathAnnotationVisitor(classPaths);
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new ControllerMethodVisitor(name, isController, className, classPaths, apis);
        }

        List<ParsedApi> getApis() {
            return apis;
        }
    }

    /** 解析方法级注解 */
    private static class ControllerMethodVisitor extends MethodVisitor {
        private final String methodName;
        private final boolean isController;
        private final String className;
        private final List<String> classPaths;
        private final List<ParsedApi> apis;

        private boolean hasMapping;
        private String httpMethod;
        private final List<String> methodPaths = new ArrayList<>();
        private final List<String> requestMappingMethods = new ArrayList<>();

        ControllerMethodVisitor(String methodName, boolean isController, String className,
                                List<String> classPaths, List<ParsedApi> apis) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            this.isController = isController;
            this.className = className;
            this.classPaths = classPaths;
            this.apis = apis;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            switch (descriptor) {
                case GET_MAPPING:
                    hasMapping = true;
                    httpMethod = "GET";
                    return new PathAnnotationVisitor(methodPaths);
                case POST_MAPPING:
                    hasMapping = true;
                    httpMethod = "POST";
                    return new PathAnnotationVisitor(methodPaths);
                case PUT_MAPPING:
                    hasMapping = true;
                    httpMethod = "PUT";
                    return new PathAnnotationVisitor(methodPaths);
                case DELETE_MAPPING:
                    hasMapping = true;
                    httpMethod = "DELETE";
                    return new PathAnnotationVisitor(methodPaths);
                case PATCH_MAPPING:
                    hasMapping = true;
                    httpMethod = "PATCH";
                    return new PathAnnotationVisitor(methodPaths);
                case REQUEST_MAPPING:
                    hasMapping = true;
                    return new RequestMappingAnnotationVisitor(methodPaths, requestMappingMethods);
                default:
                    return null;
            }
        }

        @Override
        public void visitEnd() {
            if (!isController || !hasMapping) {
                return;
            }
            List<String> httpMethods = new ArrayList<>();
            if (httpMethod != null) {
                httpMethods.add(httpMethod);
            } else if (!requestMappingMethods.isEmpty()) {
                httpMethods.addAll(requestMappingMethods);
            } else {
                // @RequestMapping 未指定 method，默认 GET
                httpMethods.add("GET");
            }
            List<String> basePaths = classPaths.isEmpty() ? List.of("") : classPaths;
            List<String> subPaths = methodPaths.isEmpty() ? List.of("") : methodPaths;
            for (String base : basePaths) {
                for (String sub : subPaths) {
                    String fullPath = combinePath(base, sub);
                    for (String m : httpMethods) {
                        apis.add(new ParsedApi(className + "#" + methodName, m, fullPath));
                    }
                }
            }
        }
    }

    /** 读取 String[] value/path 路径 */
    private static class PathAnnotationVisitor extends AnnotationVisitor {
        private final List<String> paths;

        PathAnnotationVisitor(List<String> paths) {
            super(Opcodes.ASM9);
            this.paths = paths;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("value".equals(name) || "path".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String n, Object value) {
                        if (value instanceof String) {
                            paths.add((String) value);
                        }
                    }
                };
            }
            return null;
        }
    }

    /** 读取 @RequestMapping 的 value/path 和 method 属性 */
    private static class RequestMappingAnnotationVisitor extends AnnotationVisitor {
        private final List<String> paths;
        private final List<String> methods;

        RequestMappingAnnotationVisitor(List<String> paths, List<String> methods) {
            super(Opcodes.ASM9);
            this.paths = paths;
            this.methods = methods;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("value".equals(name) || "path".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String n, Object value) {
                        if (value instanceof String) {
                            paths.add((String) value);
                        }
                    }
                };
            }
            if ("method".equals(name)) {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitEnum(String n, String descriptor, String value) {
                        methods.add(value);
                    }
                };
            }
            return null;
        }
    }
}
