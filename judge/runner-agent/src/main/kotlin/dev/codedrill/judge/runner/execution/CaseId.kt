package dev.codedrill.judge.runner.execution

import dev.codedrill.platform.problempackage.TestCase

/**
 * 하네스 프로토콜에서 케이스를 식별하는 키.
 *
 * 케이스 id 는 그룹 안에서만 유일하므로(§6.2) 그룹을 붙여 전역 유일 키를 만든다.
 */
fun TestCase.qualifiedId(): String = "$groupId/$id"
