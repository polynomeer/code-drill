package tests;

import java.util.List;
import java.util.Map;
import tmpl.Template;
import static codedrill.Assertions.*;

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. */
public class PublicTemplateTest {

    public void testVariable() {
        assertEquals("Hello, Ada!", Template.render("Hello, {{name}}!", Map.of("name", "Ada")));
    }

    public void testMissingIsEmpty() {
        assertEquals("[]", Template.render("[{{nothing}}]", Map.of()));
    }

    public void testListSection() {
        Map<String, Object> data = Map.of("items", List.of(Map.of("n", 1), Map.of("n", 2)));
        assertEquals("<1><2>", Template.render("{{#items}}<{{n}}>{{/items}}", data));
    }

    public void testUnclosedTagIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> Template.render("Hi {{name", Map.of()));
    }
}
