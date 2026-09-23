package merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// kind: OFF_BY_ONE
// 공통 조상이 끝나면 멈춘다. 두 갈래가 뒤에 덧붙인 줄이 사라진다.
public final class ThreeWayMerge {

    public static final String OURS_MARKER = "<<<<<<< ours";
    public static final String SPLIT_MARKER = "=======";
    public static final String THEIRS_MARKER = ">>>>>>> theirs";

    private ThreeWayMerge() { }

    public static MergeResult merge(List<String> base, List<String> ours, List<String> theirs) {
        if (base == null || ours == null || theirs == null) {
            throw new IllegalArgumentException("three versions are required");
        }
        int[] inOurs = align(base, ours);
        int[] inTheirs = align(base, theirs);
        List<String> out = new ArrayList<>();
        boolean conflicted = false;
        int b = 0;
        int o = 0;
        int t = 0;
        while (b < base.size()) {
            if (b < base.size() && inOurs[b] == o && inTheirs[b] == t) {
                out.add(base.get(b));
                b++; o++; t++;
                continue;
            }
            int nb = base.size();
            int no = ours.size();
            int nt = theirs.size();
            for (int i = b; i < base.size(); i++) {
                if (inOurs[i] >= o && inTheirs[i] >= t) { nb = i; no = inOurs[i]; nt = inTheirs[i]; break; }
            }
            List<String> bs = base.subList(b, nb);
            List<String> os = ours.subList(o, no);
            List<String> ts = theirs.subList(t, nt);
            if (os.equals(ts)) {
                out.addAll(os);
            } else if (bs.equals(os)) {
                out.addAll(ts);
            } else if (bs.equals(ts)) {
                out.addAll(os);
            } else {
                conflicted = true;
                out.add(OURS_MARKER);
                out.addAll(os);
                out.add(SPLIT_MARKER);
                out.addAll(ts);
                out.add(THEIRS_MARKER);
            }
            b = nb; o = no; t = nt;
        }
        return new MergeResult(conflicted, List.copyOf(out));
    }

    private static int[] align(List<String> base, List<String> other) {
        int n = base.size();
        int m = other.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = base.get(i).equals(other.get(j))
                    ? lcs[i + 1][j + 1] + 1
                    : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        int[] match = new int[n];
        Arrays.fill(match, -1);
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (base.get(i).equals(other.get(j))) { match[i] = j; i++; j++; }
            else if (lcs[i + 1][j] >= lcs[i][j + 1]) i++;
            else j++;
        }
        return match;
    }
}
