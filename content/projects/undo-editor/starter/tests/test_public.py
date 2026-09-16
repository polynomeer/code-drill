"""공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다."""

import unittest

from editor import Editor


class PublicTests(unittest.TestCase):
    def test_insert_and_delete(self):
        editor = Editor("hello")
        editor.insert(5, " world")
        self.assertEqual("hello world", editor.text)
        self.assertEqual("world", editor.delete(6, 5))
        self.assertEqual("hello ", editor.text)

    def test_undo_then_redo(self):
        editor = Editor()
        editor.insert(0, "abc")
        self.assertTrue(editor.undo())
        self.assertEqual("", editor.text)
        self.assertTrue(editor.redo())
        self.assertEqual("abc", editor.text)

    def test_out_of_range_insert(self):
        editor = Editor("ab")
        with self.assertRaises(IndexError):
            editor.insert(3, "x")
        self.assertEqual("ab", editor.text)
