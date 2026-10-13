package tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tmpl.Template;
import static codedrill.Assertions.*;

/** 숨은 테스트. 사용자에게 나가지 않는다. */
public class HiddenTemplateTest {

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> out = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i + 1]);
        return out;
    }

    public void testEscapesHtml() {
        assertEquals("&lt;b&gt;&amp;&quot;&#39;", Template.render("{{v}}", map("v", "<b>&\"'")));
    }

    public void testTripleBracesDoNotEscape() {
        assertEquals("<b>x</b>", Template.render("{{{v}}}", map("v", "<b>x</b>")));
    }

    public void testSpacesInsideTags() {
        assertEquals("Ada", Template.render("{{  name  }}", map("name", "Ada")));
        assertEquals("yes", Template.render("{{# on }}yes{{/ on }}", map("on", true)));
    }

    public void testNumbersAndTextAround() {
        assertEquals("n=42;", Template.render("n={{n}};", map("n", 42)));
        assertEquals("plain text", Template.render("plain text", map()));
    }

    public void testDottedNames() {
        Map<String, Object> data = map("user", map("name", "Ada", "address", map("city", "Seoul")));
        assertEquals("Ada/Seoul", Template.render("{{user.name}}/{{user.address.city}}", data));
        assertEquals("[]", Template.render("[{{user.missing.city}}]", data));
    }

    public void testDottedNameDoesNotFallBackOutward() {
        // 안쪽 문맥에 user 가 있으면 그 user 안에서만 찾는다
        Map<String, Object> data = map("user", map("name", "Outer"), "rows", List.of(map("user", map("id", 7))));
        assertEquals("[]", Template.render("{{#rows}}[{{user.name}}]{{/rows}}", data));
    }

    public void testFalseAndMissingSectionsAreSkipped() {
        assertEquals("ab", Template.render("a{{#off}}X{{/off}}{{#none}}Y{{/none}}b", map("off", false)));
    }

    public void testEmptyListIsSkipped() {
        assertEquals("", Template.render("{{#items}}X{{/items}}", map("items", List.of())));
    }

    public void testTruthyValueRendersOnce() {
        assertEquals("[1]", Template.render("{{#on}}[1]{{/on}}", map("on", true)));
        assertEquals("[0]", Template.render("{{#zero}}[{{zero}}]{{/zero}}", map("zero", 0)));
        assertEquals("[]", Template.render("{{#blank}}[{{blank}}]{{/blank}}", map("blank", "")));
    }

    public void testMapSectionPushesContext() {
        assertEquals("Ada", Template.render("{{#user}}{{name}}{{/user}}", map("user", map("name", "Ada"))));
    }

    public void testOuterNamesAreVisibleInsideSections() {
        Map<String, Object> data = map("sep", "|", "items", List.of(map("n", 1), map("n", 2)));
        assertEquals("1|2|", Template.render("{{#items}}{{n}}{{sep}}{{/items}}", data));
    }

    public void testInnerNameShadowsOuter() {
        Map<String, Object> data = map("n", "outer", "items", List.of(map("n", "inner"), map("m", 1)));
        assertEquals("inner,outer,", Template.render("{{#items}}{{n}},{{/items}}", data));
    }

    public void testDotIsTheCurrentItem() {
        assertEquals("a-b-c-", Template.render("{{#xs}}{{.}}-{{/xs}}", map("xs", List.of("a", "b", "c"))));
    }

    public void testInvertedSections() {
        assertEquals("none", Template.render("{{^items}}none{{/items}}", map("items", List.of())));
        assertEquals("none", Template.render("{{^missing}}none{{/missing}}", map()));
        assertEquals("none", Template.render("{{^off}}none{{/off}}", map("off", false)));
        assertEquals("", Template.render("{{^items}}none{{/items}}", map("items", List.of(1))));
        assertEquals("", Template.render("{{^on}}none{{/on}}", map("on", true)));
    }

    public void testNestedSectionsWithTheSameName() {
        Map<String, Object> data = map("a", List.of(map("a", List.of(map("v", 1), map("v", 2)))));
        assertEquals("<12>", Template.render("{{#a}}<{{#a}}{{v}}{{/a}}>{{/a}}", data));
    }

    public void testNestedLists() {
        Map<String, Object> data = map("rows", List.of(map("cells", List.of("a", "b")), map("cells", List.of("c"))));
        assertEquals("[ab][c]", Template.render("{{#rows}}[{{#cells}}{{.}}{{/cells}}]{{/rows}}", data));
    }

    public void testMismatchedCloseIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> Template.render("{{#a}}x{{/b}}", map()));
        assertThrows(IllegalArgumentException.class, () -> Template.render("x{{/a}}", map()));
    }

    public void testUnclosedSectionIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> Template.render("{{#a}}x", map()));
    }

    public void testUnclosedTripleIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> Template.render("{{{raw}}", map()));
    }

    public void testSingleBracesAreText() {
        assertEquals("{x} }}", Template.render("{x} }}", map()));
    }
}
