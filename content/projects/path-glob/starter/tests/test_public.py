"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from pathglob import match


class PublicTests(unittest.TestCase):
    def test_literal_and_question_mark(self):
        self.assertTrue(match("a?c", "abc"))
        self.assertFalse(match("a?c", "ac"))
        self.assertTrue(match("docs/readme.md", "docs/readme.md"))
        self.assertFalse(match("docs/readme.md", "docs/README.md"))

    def test_star_stays_in_one_segment(self):
        self.assertTrue(match("*.py", "main.py"))
        self.assertFalse(match("*.py", "src/main.py"))
        self.assertTrue(match("src/*/main.py", "src/app/main.py"))

    def test_globstar_spans_directories(self):
        self.assertTrue(match("src/**/test.py", "src/a/b/test.py"))
        self.assertFalse(match("src/**/test.py", "lib/a/test.py"))

    def test_simple_class(self):
        self.assertTrue(match("[abc].txt", "b.txt"))
        self.assertFalse(match("[abc].txt", "d.txt"))


if __name__ == "__main__":
    unittest.main()
