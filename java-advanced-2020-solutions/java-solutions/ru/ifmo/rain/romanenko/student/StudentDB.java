package ru.ifmo.rain.romanenko.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {
    private List<Group> getSpecialSortedList(final Stream<Map.Entry<String, List<Student>>> groupsStream, final UnaryOperator<List<Student>> sort) {
        return groupsStream.map(elem -> new Group(elem.getKey(), sort.apply(elem.getValue())))
                .collect(Collectors.toList());
    }

    private String fullName(final Student student) {
        return student.getFirstName() + ' ' + student.getLastName();
    }

    private Stream<Map.Entry<String, List<Student>>> getStream(final Collection<Student> students, final Supplier<Map<String, List<Student>>> mapSupplier) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getGroup, mapSupplier, Collectors.toList()))
                .entrySet().stream();
    }

    private String getBiggestGroupFilter(final Stream<Map.Entry<String, List<Student>>> groupsStream, final ToIntFunction<List<Student>> filter) {
        return groupsStream
                .max(Comparator.comparingInt((Map.Entry<String, List<Student>> group) -> filter.applyAsInt(group.getValue()))
                        .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    private <T, C extends Collection<T>> C StudentsCollectionMapped(final Collection<Student> students, final Function<Student, T> mapper, final Supplier<C> collection) {
        return students.stream().map(mapper).collect(Collectors.toCollection(collection));
    }

    private <T> List<T> StudentsSpecialMappedList(final Collection<Student> students, final Function<Student, T> mapper) {
        return StudentsCollectionMapped(students, mapper, ArrayList::new);
    }

    private List<Student> sortedStudents(final Stream<Student> studentStream, final Comparator<Student> cmp) {
        return studentStream.sorted(cmp).collect(Collectors.toList());
    }

    private List<Student> simplySortedStudents(final Collection<Student> students, final Comparator<Student> cmp) {
        return sortedStudents(students.stream(), cmp);
    }

    private Stream<Student> filteredSpecialStudentsStream(final Collection<Student> students, final Predicate<Student> predicate) {
        return students.stream().filter(predicate);
    }

    private final Comparator<Student> dataComp = Comparator.comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);

    private List<Student> filterAndSortByName(final Collection<Student> students, final Predicate<Student> predicate) {
        return sortedStudents(filteredSpecialStudentsStream(students, predicate), dataComp);
    }

    private Predicate<Student> getGroupPredicate(final String group) {
        return student -> group.equals(student.getGroup());
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getSpecialSortedList(getStream(students, TreeMap::new), this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getSpecialSortedList(getStream(students, TreeMap::new), this::sortStudentsById);
    }

    @Override
    public String getLargestGroup(final Collection<Student> students) {
        return getBiggestGroupFilter(getStream(students, HashMap::new), List::size);
    }

    @Override
    public String getLargestGroupFirstName(final Collection<Student> students) {
        return getBiggestGroupFilter(getStream(students, HashMap::new), studentsList -> getDistinctFirstNames(studentsList).size());
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return StudentsSpecialMappedList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return StudentsSpecialMappedList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final List<Student> students) {
        return StudentsSpecialMappedList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return StudentsSpecialMappedList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return StudentsCollectionMapped(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(final List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return simplySortedStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return simplySortedStudents(students, dataComp);
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return filterAndSortByName(students, student -> name.equals(student.getFirstName()));
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return filterAndSortByName(students, student -> name.equals(student.getLastName()));
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final String group) {
        return filterAndSortByName(students, getGroupPredicate(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final String group) {
        return filteredSpecialStudentsStream(students, getGroupPredicate(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(this::fullName, Collectors.mapping(Student::getGroup, Collectors.toSet())))
                .entrySet().stream().max(Map.Entry.<String, Set<String>>comparingByValue(Comparator.comparingInt(Set::size)).thenComparing(Map.Entry.comparingByKey(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    private List<String> getObj(final List<String> names, final int[] indices) {
        return Arrays.stream(indices).mapToObj(names::get).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getObj(StudentsSpecialMappedList(students, Student::getFirstName), indices);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getObj(StudentsSpecialMappedList(students, Student::getLastName), indices);
    }

    @Override
    public List<String> getGroups(final Collection<Student> students, final int[] indices) {
        return getObj(StudentsSpecialMappedList(students, Student::getGroup), indices);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getObj(StudentsSpecialMappedList(students, this::fullName), indices);
    }
}