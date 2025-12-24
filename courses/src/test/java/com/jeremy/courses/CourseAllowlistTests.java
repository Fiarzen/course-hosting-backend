package com.jeremy.courses;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CourseAllowlistTests {

    @Test
    void unrestrictedCourseAllowsAnyEmail() {
        Course course = new Course();
        course.setRestrictedToAllowList(false);
        course.setAllowedEmails(Set.of("alice@example.com"));

        assertTrue(course.isEmailAllowed("someone@example.com"));
        assertTrue(course.isEmailAllowed(null)); // helper treats unrestricted as globally allowed
    }

    @Test
    void restrictedCourseOnlyAllowsEmailsOnAllowlist_caseInsensitive() {
        Course course = new Course();
        course.setRestrictedToAllowList(true);
        course.setAllowedEmails(Set.of("allowed@example.com"));

        assertTrue(course.isEmailAllowed("allowed@example.com"));
        assertTrue(course.isEmailAllowed("ALLOWED@example.com"));
        assertFalse(course.isEmailAllowed("other@example.com"));
    }

    @Test
    void restrictedCourseWithNullAllowlistDeniesEveryone() {
        Course course = new Course();
        course.setRestrictedToAllowList(true);
        course.setAllowedEmails(null);

        assertFalse(course.isEmailAllowed("anyone@example.com"));
    }
}
