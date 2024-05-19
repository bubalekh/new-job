package com.example.test.copyutils.model;

import com.example.test.copyutils.service.TestService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Man {

    private static final String test = "test";
    public boolean testBoolean = false;
    private final String testString;
    private String name;
    private int age;
    private Map<String, Integer> favoriteBooksMap;
    public List<String> favoriteBooksList;
    private final Man man;
    private final Object array;
    private final TestService testImpl;

    public Man(String testString,
               String name,
               int age,
               Map<String, Integer> favoriteBooksMap,
               Man man,
               Object array,
               TestService testImpl) {
        this.testString = testString;
        this.name = name;
        this.age = age;
        this.favoriteBooksMap = favoriteBooksMap;
        this.man = man;
        this.array = array;
        this.testImpl = testImpl;
    }

    public String getTestString() {
        return testString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Map<String, Integer> getFavoriteBooksMap() {
        return favoriteBooksMap;
    }

    public void setFavoriteBooksMap(Map<String, Integer> favoriteBooksMap) {
        this.favoriteBooksMap = favoriteBooksMap;
    }

    public Man getMan() {
        return man;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Man man1 = (Man) o;

        if (testBoolean != man1.testBoolean) return false;
        if (age != man1.age) return false;
        if (!Objects.equals(testString, man1.testString)) return false;
        if (!Objects.equals(name, man1.name)) return false;
        if (!Objects.equals(favoriteBooksMap, man1.favoriteBooksMap))
            return false;
        if (!Objects.equals(favoriteBooksList, man1.favoriteBooksList))
            return false;
        return Objects.equals(man, man1.man);
        // I purposely remove array from equals method because of AutoBoxing of Array.get() method
//        return Objects.equals(array, man1.array);
    }

    @Override
    public int hashCode() {
        int result = (testBoolean ? 1 : 0);
        result = 31 * result + (testString != null ? testString.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + (favoriteBooksMap != null ? favoriteBooksMap.hashCode() : 0);
        result = 31 * result + (favoriteBooksList != null ? favoriteBooksList.hashCode() : 0);
        result = 31 * result + (man != null ? man.hashCode() : 0);
        // I purposely remove array from hashCode method because of AutoBoxing of Array.get() method
//        result = 31 * result + (array != null ? array.hashCode() : 0);
        return result;
    }

    public Object getArray() {
        return array;
    }

    public TestService getTestImpl() {
        return testImpl;
    }
}