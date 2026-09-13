package dev.codedrill.judge.runner

import dev.codedrill.judge.runner.execution.KotlinCompilerArchive
import dev.codedrill.judge.runner.execution.adapter.KotlinAdapter
import java.nio.file.Path

/**
 * 테스트가 공유하는 Kotlin 어댑터. 컴파일마다 JVM 이 뜨므로(§5.5) 클래스 아카이브를 한 번
 * 만들어 두고 모든 테스트 클래스가 쓴다. 아카이브는 build/ 아래에 남아 다음 실행에도 쓰인다.
 */
object TestAdapters {
    val kotlin: KotlinAdapter by lazy { KotlinCompilerArchive.warmedAdapter(Path.of("build/kotlin-cds")) }
}
