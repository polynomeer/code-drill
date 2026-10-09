"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from pathglob import match


class Segments(unittest.TestCase):
    def test_question_mark_is_not_a_slash(self):
        self.assertFalse(match("a?b", "a/b"))

    def test_star_matches_empty(self):
        self.assertTrue(match("a*b", "ab"))
        self.assertTrue(match("a*", "a"))
        self.assertTrue(match("*", "anything"))

    def test_star_never_crosses_a_slash(self):
        self.assertFalse(match("a*", "ab/c"))
        self.assertFalse(match("*", "a/b"))

    def test_double_star_inside_a_segment_is_a_star(self):
        self.assertTrue(match("a**b", "axxb"))
        self.assertFalse(match("a**b", "ax/xb"))

    def test_whole_path_must_match(self):
        self.assertFalse(match("src", "src/main.py"))
        self.assertFalse(match("src/main.py", "src"))

    def test_case_sensitive(self):
        self.assertFalse(match("*.PY", "main.py"))

    def test_many_stars_stay_fast(self):
        self.assertFalse(match("*a*a*a*a*a*a*a*a*a*a*b", "a" * 200))
        self.assertTrue(match("*a*a*a*a*a*a*a*a*a*a*b", "a" * 200 + "b"))


class Globstar(unittest.TestCase):
    def test_matches_zero_segments(self):
        self.assertTrue(match("a/**/b", "a/b"))
        self.assertTrue(match("**/b", "b"))
        self.assertTrue(match("a/**", "a"))

    def test_matches_many_segments(self):
        self.assertTrue(match("a/**/b", "a/x/y/z/b"))
        self.assertTrue(match("**", "a/b/c"))
        self.assertTrue(match("**/*.py", "a/b/c.py"))

    def test_two_globstars(self):
        self.assertTrue(match("**/x/**/y", "x/y"))
        self.assertTrue(match("**/x/**/y", "a/x/b/c/y"))
        self.assertFalse(match("**/x/**/y", "a/y/b/x"))

    def test_still_needs_the_rest(self):
        self.assertFalse(match("a/**/b", "a/x/c"))
        self.assertFalse(match("a/**/b", "b"))


class HiddenEntries(unittest.TestCase):
    def test_wildcards_skip_leading_dot(self):
        self.assertFalse(match("*", ".env"))
        self.assertFalse(match("?env", ".env"))
        self.assertFalse(match("[.]env", ".env"))
        self.assertFalse(match("*/config", ".git/config"))

    def test_literal_dot_matches(self):
        self.assertTrue(match(".*", ".env"))
        self.assertTrue(match(".git/config", ".git/config"))
        self.assertTrue(match(r"\.env", ".env"))

    def test_dot_inside_is_ordinary(self):
        self.assertTrue(match("*", "a.b.c"))
        self.assertTrue(match("a?b", "a.b"))

    def test_globstar_skips_hidden_directories(self):
        self.assertFalse(match("**/config", ".git/config"))
        self.assertFalse(match("**/*.py", "src/.cache/x.py"))
        self.assertTrue(match("**/.git/config", "repo/.git/config"))
        self.assertTrue(match("**/*.py", "src/pkg/x.py"))

    def test_globstar_still_reaches_a_hidden_last_segment_only_by_literal_dot(self):
        self.assertFalse(match("**", ".env"))
        self.assertTrue(match("**/.env", "a/b/.env"))


class Classes(unittest.TestCase):
    def test_range(self):
        self.assertTrue(match("file[0-9].txt", "file7.txt"))
        self.assertFalse(match("file[0-9].txt", "file-.txt"))
        self.assertTrue(match("[a-cx-z]", "y"))
        self.assertFalse(match("[a-cx-z]", "d"))

    def test_negation(self):
        self.assertTrue(match("[!a]", "b"))
        self.assertFalse(match("[!a]", "a"))
        self.assertFalse(match("[!a-z]", "q"))
        self.assertTrue(match("[!a-z]", "Q"))

    def test_bracket_first_is_literal(self):
        self.assertTrue(match("[]a]", "]"))
        self.assertTrue(match("[]a]", "a"))
        self.assertTrue(match("[!]]", "x"))
        self.assertFalse(match("[!]]", "]"))

    def test_dash_at_the_edges_is_literal(self):
        self.assertTrue(match("[a-]", "-"))
        self.assertTrue(match("[-a]", "-"))
        self.assertFalse(match("[a-]", "b"))

    def test_reversed_range_matches_nothing(self):
        self.assertFalse(match("[z-a]", "m"))

    def test_class_never_matches_a_slash(self):
        self.assertFalse(match("a[/]b", "a/b"))
        self.assertFalse(match("a[!x]b", "a/b"))

    def test_unclosed_bracket_is_a_letter(self):
        self.assertTrue(match("[a", "[a"))
        self.assertTrue(match("x[", "x["))
        self.assertFalse(match("[a", "a"))


class Escapes(unittest.TestCase):
    def test_escaped_wildcards_are_letters(self):
        self.assertTrue(match(r"\*", "*"))
        self.assertFalse(match(r"\*", "a"))
        self.assertTrue(match(r"what\?", "what?"))
        self.assertFalse(match(r"what\?", "whats"))
        self.assertTrue(match(r"\[a]", "[a]"))

    def test_escaped_backslash(self):
        self.assertTrue(match("a\\\\b", "a\\b"))

    def test_trailing_backslash_is_a_letter(self):
        self.assertTrue(match("a\\", "a\\"))

    def test_escape_inside_class(self):
        self.assertTrue(match(r"[\]]", "]"))
        self.assertTrue(match(r"[\!a]", "!"))


if __name__ == "__main__":
    unittest.main()
