package split

// kind: OFF_BY_ONE
// 봉사료의 소수점 아래를 버린다. 0.5원은 올려야 한다.

/**
 * 계산서 나누기. 금액은 모두 **원 단위 정수**(Long)다 — 나눈 몫의 합은 언제나 원래 금액과 같아야 한다.
 */
class BillSplitter {

    /** 똑같이 나눈다. 나누어떨어지지 않는 원은 앞사람부터 1원씩 더 낸다. */
    fun splitEvenly(total: Long, people: List<String>): List<Share> {
        checkTotal(total)
        checkNames(people)
        val base = total / people.size
        val extra = (total % people.size).toInt()
        return people.mapIndexed { i, name -> Share(name, base + if (i < extra) 1 else 0) }
    }

    /**
     * 가중치에 비례해 나눈다. 각자 정확한 몫의 내림을 먼저 주고, 남은 원은 버린 소수 부분이 큰 사람부터 1원씩
     * 준다 — 같으면 앞사람. 곱셈은 정수로 한다: 10^12 × 10^6 은 Long 에 들고, Double 은 1원을 잃는다.
     */
    fun splitByWeight(total: Long, weights: List<Pair<String, Int>>): List<Share> {
        checkTotal(total)
        checkNames(weights.map { it.first })
        for ((_, w) in weights) require(w in 0..MAX_WEIGHT) { "weight out of range: $w" }
        val sum = weights.sumOf { it.second.toLong() }
        require(sum > 0) { "weights must not all be zero" }
        val base = LongArray(weights.size) { total * weights[it].second / sum }
        val remainder = LongArray(weights.size) { total * weights[it].second % sum }
        var left = total - base.sum()
        val order = weights.indices.sortedWith(compareByDescending<Int> { remainder[it] }.thenBy { it })
        for (i in order) {
            if (left == 0L) break
            base[i] += 1
            left -= 1
        }
        return weights.mapIndexed { i, (name, _) -> Share(name, base[i]) }
    }

    /** 봉사료를 더한 금액. 봉사료는 원 단위로 반올림한다 — 0.5원은 올린다. */
    fun addTip(total: Long, percent: Int): Long {
        checkTotal(total)
        require(percent in 0..100) { "percent out of range: $percent" }
        return total + total * percent / 100
    }

    private fun checkTotal(total: Long) {
        require(total in 0..MAX_TOTAL) { "total out of range: $total" }
    }

    private fun checkNames(names: List<String>) {
        require(names.isNotEmpty()) { "nobody to split with" }
        require(names.none { it.isBlank() }) { "blank name" }
        require(names.toSet().size == names.size) { "duplicate name" }
    }

    companion object {
        const val MAX_TOTAL = 1_000_000_000_000L
        const val MAX_WEIGHT = 1_000_000
    }
}
