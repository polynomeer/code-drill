# kind: MISSING_EDGE_CASE
# 재귀로 따라간다. 칸이 천 개 넘게 이어지면 파이썬의 재귀 한도를 넘는다.
"""작은 스프레드시트. 값은 읽을 때마다 원본에서 다시 계산한다 — 저장하는 것은 입력뿐이다."""

import re

CYCLE = "#CYCLE!"
DIV0 = "#DIV/0!"

_CELL = re.compile(r"[A-Z][1-9][0-9]?")
_INTEGER = re.compile(r"-?[0-9]+")
_TOKEN = re.compile(r"\s*(?:(SUM)|([A-Z][1-9][0-9]?)|([0-9]+)|(.))")


class _DivisionByZero(Exception):
    pass


def _check_cell(cell):
    if not isinstance(cell, str) or not _CELL.fullmatch(cell):
        raise ValueError(f"bad cell: {cell!r}")


def _tokens(text):
    out = []
    for word, cell, number, other in _TOKEN.findall(text):
        if word:
            out.append(("SUM", word))
        elif cell:
            out.append(("CELL", cell))
        elif number:
            out.append(("NUM", int(number)))
        elif other.strip():
            if other not in "+-*/():":
                raise ValueError(f"bad character: {other!r}")
            out.append((other, other))
    return out


class _Parser:
    """식 → 나무. expr = term (+|- term)*, term = unary (*|/ unary)*, unary = -unary | primary."""

    def __init__(self, text):
        self.tokens = _tokens(text)
        self.at = 0

    def peek(self):
        return self.tokens[self.at][0] if self.at < len(self.tokens) else None

    def take(self, kind):
        if self.peek() != kind:
            raise ValueError(f"expected {kind}")
        token = self.tokens[self.at]
        self.at += 1
        return token[1]

    def parse(self):
        if not self.tokens:
            raise ValueError("empty formula")
        tree = self.expr()
        if self.at != len(self.tokens):
            raise ValueError("trailing input")
        return tree

    def expr(self):
        tree = self.term()
        while self.peek() in ("+", "-"):
            op = self.take(self.peek())
            tree = ("bin", op, tree, self.term())
        return tree

    def term(self):
        tree = self.unary()
        while self.peek() in ("*", "/"):
            op = self.take(self.peek())
            tree = ("bin", op, tree, self.unary())
        return tree

    def unary(self):
        if self.peek() == "-":
            self.take("-")
            return ("neg", self.unary())
        return self.primary()

    def primary(self):
        kind = self.peek()
        if kind == "NUM":
            return ("num", self.take("NUM"))
        if kind == "CELL":
            return ("ref", self.take("CELL"))
        if kind == "SUM":
            self.take("SUM")
            self.take("(")
            first = self.take("CELL")
            self.take(":")
            last = self.take("CELL")
            self.take(")")
            return ("sum", first, last)
        if kind == "(":
            self.take("(")
            tree = self.expr()
            self.take(")")
            return tree
        raise ValueError("expected a value")


def _range(first, last):
    c1, c2 = sorted((first[0], last[0]))
    r1, r2 = sorted((int(first[1:]), int(last[1:])))
    return [f"{chr(c)}{r}" for c in range(ord(c1), ord(c2) + 1) for r in range(r1, r2 + 1)]


def _refs(tree):
    kind = tree[0]
    if kind == "ref":
        return [tree[1]]
    if kind == "sum":
        return _range(tree[1], tree[2])
    if kind == "neg":
        return _refs(tree[1])
    if kind == "bin":
        return _refs(tree[2]) + _refs(tree[3])
    return []


def _divide(a, b):
    if b == 0:
        raise _DivisionByZero()
    q = abs(a) // abs(b)
    return q if (a >= 0) == (b > 0) else -q


class Sheet:
    def __init__(self):
        self._inputs = {}

    def set(self, cell, text):
        _check_cell(cell)
        if not isinstance(text, str):
            raise ValueError("text must be a string")
        text = text.strip()
        if text == "":
            self._inputs.pop(cell, None)
        elif text.startswith("="):
            self._inputs[cell] = _Parser(text[1:]).parse()
        elif _INTEGER.fullmatch(text):
            self._inputs[cell] = ("num", int(text))
        else:
            raise ValueError(f"not a number or a formula: {text!r}")

    def get(self, cell):
        _check_cell(cell)
        return self._evaluate(cell)

    def _deps(self, cell):
        tree = self._inputs.get(cell)
        return _refs(tree) if tree else []

    def _evaluate(self, target):
        values = {}
        on_path = set()

        def visit(cell):
            if cell in values:
                return values[cell]
            if cell in on_path:
                return CYCLE
            on_path.add(cell)
            found = [visit(d) for d in self._deps(cell)]
            on_path.discard(cell)
            values[cell] = CYCLE if CYCLE in found else self._compute(cell, values)
            return values[cell]

        return visit(target)

    def _compute(self, cell, values):
        tree = self._inputs.get(cell)
        if tree is None:
            return 0

        def walk(node):
            kind = node[0]
            if kind == "num":
                return node[1]
            if kind == "ref":
                value = values[node[1]]
                if value == DIV0:
                    raise _DivisionByZero()
                return value
            if kind == "sum":
                total = 0
                for ref in _range(node[1], node[2]):
                    if values[ref] == DIV0:
                        raise _DivisionByZero()
                    total += values[ref]
                return total
            if kind == "neg":
                return -walk(node[1])
            left, right = walk(node[2]), walk(node[3])
            if node[1] == "+":
                return left + right
            if node[1] == "-":
                return left - right
            if node[1] == "*":
                return left * right
            return _divide(left, right)

        try:
            return walk(tree)
        except _DivisionByZero:
            return DIV0
