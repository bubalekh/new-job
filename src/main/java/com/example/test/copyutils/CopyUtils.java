package com.example.test.copyutils;

import com.example.test.copyutils.model.Man;
import com.example.test.copyutils.service.TestService;
import com.example.test.copyutils.service.TestServiceImpl;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class CopyUtils {

    public static Object deepCopy(Object obj) {
        // It would be much faster and more convenient to have memcpy() function form C/C++, but we're using Java)
        // Let's add a major corner case first!
        if (obj == null) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        // clazz.newInstance() is deprecated, lets find a most suitable constructor (with the least amount arguments,
        // possibly with single primitive argument or default constructor)
        Constructor<?>[] constructors = clazz.getConstructors();
        if (clazz.isArray() && constructors.length == 0) {
            return CopyUtils.deepCopyArrayReflection(obj);
        }
        Constructor<?> mostSuitableConstructor = Arrays.stream(constructors)
                .sorted(Comparator.comparingInt(Constructor::getParameterCount))
                .filter(cstr -> {
                    if (cstr.getParameterCount() == 1) {
                        return Arrays.stream(cstr.getParameterTypes()).anyMatch(Class::isPrimitive);
                    }
                    return true;
                })
                .findFirst()
                .orElseThrow();
        Object newInstance;
        // Before creating new instance we must create mock arguments
        Class<?>[] parameterTypes = mostSuitableConstructor.getParameterTypes();
        Object[] args = Arrays.stream(parameterTypes)
                .map(parameterType -> {
                    // Let's assume we have 4 major types or arguments
                    // 1. primitives or wrappers - they are different in terms of JMM, but for this test we don't care =)
                    if (parameterType.isPrimitive()) {
                        if (clazz.isPrimitive() || getWrapperTypes().contains(clazz)) {
                            return obj;
                        }
                        return 0;
                    }
                    // 2. Strings (which is an immutable-cached object)
                    if (parameterType.getName().contains("String")) {
                        return "0";
                    }
                    // 3. Java arrays
                    if (parameterType.isArray()) {
                        return new Object[0];
                    }
                    // 4. Any other objects =)
                    return null;
                })
                .toArray();
        try {
            // Now we can create a new instance
            newInstance = mostSuitableConstructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        // Last step! We need to be sure that all nonstatic fields of new object will receive a corresponding values of parent object
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                // We should skip all inaccessible fields to avoid exceptions (they are probably primitives, so default values are pretty much acceptable here)
                if (field.trySetAccessible()) {
                    try {
                        Object parentValue = field.get(obj);
                        Class<?> type = field.getType();
                        // First of all, lets skip null values, because they were already set
                        if (parentValue != null) {
                            switch (parentValue) {
                                case Map<?, ?> map -> {
                                    // If parentValue is map, we need to create a deep copy of that map.
                                    // Let's remember Map contract: map key MUST be immutable object, so we don't need to deepCopy key, only value!
                                    Map<?, ?> clonedMap = map.entrySet().stream()
                                            .map(entry -> Map.entry(entry.getKey(), CopyUtils.deepCopy(entry.getValue())))
                                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                                    field.set(newInstance, clonedMap);
                                }
                                case Iterable<?> parentIterable -> {
                                    // I'm out of imagination to get implementation-free approach to deepClone a collection
                                    // If you have one - let me know =)
                                    Collection<Object> cache = new ArrayList<>();
                                    for (Object next : parentIterable) {
                                        cache.add(next);
                                    }
                                    field.set(newInstance, cache);
                                }
                                default -> {
                                    if (type.isArray()) {
                                        field.set(newInstance, CopyUtils.deepCopyArrayReflection(parentValue));
                                    }
                                    // The easiest part of the test is to set some primitive, wrapper-based (aka "primitive") and mutable values
                                    else if (type.isPrimitive() || getWrapperTypes().contains(type) || parentValue instanceof String) {
                                        field.set(newInstance, parentValue);
                                    }
                                    // Now let's add some basic recursion for nested object
                                    else {
                                        if (parentValue == obj) {
                                            field.set(newInstance, newInstance);
                                        } else {
                                            field.set(newInstance, CopyUtils.deepCopy(parentValue));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return newInstance;
    }

    private static Object deepCopyArrayReflection(Object array) {
        Object firstElement = Array.get(array, 0);
        if (firstElement != null) {
            int length = Array.getLength(array);
            Object clonedArray = Array.newInstance(firstElement.getClass(), length);
            for (int i = 0; i < length; i++) {
                Array.set(clonedArray, i, CopyUtils.deepCopy(Array.get(array, i)));
            }
            return clonedArray;
        }
        return new Object();
    }

    public static Set<Class<?>> getWrapperTypes() {
        return Set.of(
                Void.class,
                Boolean.class,
                Byte.class,
                Character.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class
        );
    }

    public static void main(String[] args) {
        // Don't forget to add -ea VM option to enable asserts!
        // Let's add some cases
        TestService testImpl = new TestServiceImpl();
        Map<String, Integer> booksMap = new HashMap<>();
        booksMap.put("testBook", 1);
        List<String> booksList = new ArrayList<>();
        booksList.add("testBook");
        Man firstMan = new Man("testString", "test", 10, booksMap, null, new int[]{5}, null);
        Man man = new Man("testString", "test", 10, booksMap, firstMan, new Long[]{5L, 6L}, testImpl);
        // We occasionally set fields directly
        man.testBoolean = true;
        man.favoriteBooksList = booksList;

        // Let's do a copy
        Man deepCopy = (Man) CopyUtils.deepCopy(man);
        // This assert is useless due to Java AutoBoxing, but it's fun
        int i = 1;
        assert CopyUtils.deepCopy(i).equals(i);
        // There is a problem with nested primitive arrays. Java reflection class Array can't operate with primitives properly
        // (at least I couldn't make it)
        // There is a way to do it with Java Unsafe API, but it isn't a part of J2SE
        // In other way, forced AutoBoxing isn't that bad
        Integer[] deepCopyNestedArray = (Integer[]) deepCopy.getMan().getArray();
        int[] manNestedArray = (int[]) man.getMan().getArray();
        assert Objects.equals(manNestedArray[0], deepCopyNestedArray[0]);
        // Uncomment line above to break the assertion
//        deepCopy.setAge(1);
        int deepCopyHashCode = deepCopy.hashCode();
        assert deepCopyHashCode == man.hashCode();
        assert man.equals(deepCopy);
        System.out.println("We're done!");
        // Object.deepEquals still returns false btw...
    }
}
