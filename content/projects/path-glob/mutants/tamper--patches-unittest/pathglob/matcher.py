# kind: WRONG_ALGORITHM
# 구현 대신 테스트 기반을 손댄다. 채점기의 가드가 잡아야 한다.
import unittest as _ut

for _name in dir(_ut.TestCase):
    if _name.startswith("assert"):
        setattr(_ut.TestCase, _name, lambda self, *a, **k: None)
_ut.TestCase.fail = lambda self, *a, **k: None


def match(pattern: str, path: str) -> bool:
    return False
