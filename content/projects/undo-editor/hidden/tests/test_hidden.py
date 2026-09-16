"""숨은 테스트. 사용자에게 나가지 않는다."""

import unittest

from editor import Editor


class UndoRedo(unittest.TestCase):
    def test_new_edit_clears_redo(self):
        editor = Editor()
        editor.insert(0, "abc")
        editor.undo()
        editor.insert(0, "x")
        self.assertFalse(editor.redo())
        self.assertEqual("x", editor.text)

    def test_undo_delete_restores_at_same_position(self):
        editor = Editor("abcdef")
        editor.delete(2, 3)
        self.assertEqual("abf", editor.text)
        self.assertTrue(editor.undo())
        self.assertEqual("abcdef", editor.text)
        self.assertTrue(editor.redo())
        self.assertEqual("abf", editor.text)

    def test_alternating_undo_redo_stays_consistent(self):
        editor = Editor()
        editor.insert(0, "a")
        editor.insert(1, "b")
        editor.insert(2, "c")
        self.assertTrue(editor.undo())
        self.assertTrue(editor.undo())
        self.assertEqual("a", editor.text)
        self.assertTrue(editor.redo())
        self.assertEqual("ab", editor.text)
        self.assertTrue(editor.undo())
        self.assertTrue(editor.redo())
        self.assertTrue(editor.redo())
        self.assertEqual("abc", editor.text)
        self.assertFalse(editor.redo())

    def test_undo_with_nothing_returns_false(self):
        editor = Editor("x")
        self.assertFalse(editor.undo())
        self.assertFalse(editor.redo())
        self.assertEqual("x", editor.text)

    def test_empty_insert_is_not_an_edit(self):
        editor = Editor("ab")
        editor.insert(1, "")
        self.assertEqual("ab", editor.text)
        self.assertFalse(editor.undo())
        self.assertEqual([], editor.history())


class Validation(unittest.TestCase):
    def test_delete_beyond_end_changes_nothing(self):
        editor = Editor("abc")
        with self.assertRaises(IndexError):
            editor.delete(1, 5)
        self.assertEqual("abc", editor.text)
        self.assertEqual([], editor.history())

    def test_delete_rejects_zero_length_and_negative_position(self):
        editor = Editor("abc")
        with self.assertRaises(IndexError):
            editor.delete(0, 0)
        with self.assertRaises(IndexError):
            editor.delete(-1, 1)

    def test_failed_insert_leaves_redo_intact(self):
        editor = Editor()
        editor.insert(0, "abc")
        editor.undo()
        with self.assertRaises(IndexError):
            editor.insert(5, "x")
        self.assertTrue(editor.redo())
        self.assertEqual("abc", editor.text)


class History(unittest.TestCase):
    def test_history_lists_oldest_first_without_undone(self):
        editor = Editor()
        editor.insert(0, "abc")
        editor.delete(0, 1)
        editor.insert(2, "z")
        editor.undo()
        self.assertEqual(["insert@0:'abc'", "delete@0:'a'"], editor.history())

    def test_history_is_a_copy(self):
        editor = Editor()
        editor.insert(0, "a")
        editor.history().clear()
        self.assertEqual(["insert@0:'a'"], editor.history())
