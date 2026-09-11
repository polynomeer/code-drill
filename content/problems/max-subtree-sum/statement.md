# 가장 큰 서브트리 합

정점 `0..n-1` 의 트리가 부모 배열 `parent` 로, 정점의 값이 `values` 로 주어진다. 어떤
정점을 루트로 하는 서브트리(그 정점과 모든 자손)의 값 합 중 **최댓값**을 반환한다.

값은 음수일 수 있다. 잎 하나짜리 서브트리도 서브트리다.

```kotlin
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int
```

자식이 부모보다 먼저 처리돼야 한다. 깊은 정점부터 정리하거나, 자식 목록을 만들어 뒤에서
훑는다. 부모 배열의 순서는 보장이 없다.

## 실행 리플레이 계측 (선택)

풀이 과정을 시각적으로 되짚고 싶으면 `Drill` SDK 를 호출한다. 채점 실행에서는
no-op 으로 컴파일되므로 시간·메모리 판정에 영향을 주지 않는다.

```kotlin
Drill.node(i)                  // 정점 i 의 서브트리 합이 확정됐다
Drill.edge(i, parent)          // 부모에 합을 올려 보냈다
Drill.match(i, total)          // 최댓값이 갱신됐다
```

## 제약

- `1 <= parent.size = values.size <= 200_000`
- `-10^4 <= values[i] <= 10^4`, 합은 `Int` 범위 안
