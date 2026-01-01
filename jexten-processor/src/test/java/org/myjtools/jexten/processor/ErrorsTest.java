package org.myjtools.jexten.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


@DisplayName("Errors")
class ErrorsTest {

    private Errors errors;

    @BeforeEach
    void setUp() {
        errors = new Errors();
    }


    @Test
    @DisplayName("should start with no messages")
    void shouldStartWithNoMessages() {
        assertThat(errors.hasMessages()).isFalse();
        assertThat(errors.messages()).isEmpty();
    }


    @Test
    @DisplayName("should start with no fixes")
    void shouldStartWithNoFixes() {
        assertThat(errors.hasFixes()).isFalse();
        assertThat(errors.fixes()).isEmpty();
    }


    @Test
    @DisplayName("should add message for element")
    void shouldAddMessageForElement() {
        Element element = mock(Element.class);

        errors.addMessage(element, "Error in {} at line {}", "MyClass", 42);

        assertThat(errors.hasMessages()).isTrue();
        assertThat(errors.messages()).containsKey(element);
        assertThat(errors.messages().get(element))
            .containsExactly("Error in MyClass at line 42");
    }


    @Test
    @DisplayName("should add multiple messages for same element")
    void shouldAddMultipleMessagesForSameElement() {
        Element element = mock(Element.class);

        errors.addMessage(element, "First error");
        errors.addMessage(element, "Second error");

        assertThat(errors.messages().get(element))
            .containsExactly("First error", "Second error");
    }


    @Test
    @DisplayName("should add messages for different elements")
    void shouldAddMessagesForDifferentElements() {
        Element element1 = mock(Element.class);
        Element element2 = mock(Element.class);

        errors.addMessage(element1, "Error 1");
        errors.addMessage(element2, "Error 2");

        assertThat(errors.messages()).hasSize(2);
        assertThat(errors.messages().get(element1)).containsExactly("Error 1");
        assertThat(errors.messages().get(element2)).containsExactly("Error 2");
    }


    @Test
    @DisplayName("should add fix suggestion")
    void shouldAddFixSuggestion() {
        errors.addFix("exports {};", "com.example");

        assertThat(errors.hasFixes()).isTrue();
        assertThat(errors.fixes()).containsExactly("exports com.example;");
    }


    @Test
    @DisplayName("should add multiple fix suggestions")
    void shouldAddMultipleFixSuggestions() {
        errors.addFix("exports {};", "com.example");
        errors.addFix("uses {};", "com.example.Service");

        assertThat(errors.fixes())
            .containsExactly("exports com.example;", "uses com.example.Service;");
    }


    @Test
    @DisplayName("should format message with multiple placeholders")
    void shouldFormatMessageWithMultiplePlaceholders() {
        Element element = mock(Element.class);

        errors.addMessage(element, "{} must implement {} version {}", "MyExt", "Service", "1.0");

        assertThat(errors.messages().get(element))
            .containsExactly("MyExt must implement Service version 1.0");
    }
}
