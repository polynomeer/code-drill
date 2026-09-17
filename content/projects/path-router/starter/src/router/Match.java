package router;

import java.util.Map;

/** 맞은 결과. 핸들러 이름과 매개변수 이름 → 조각. 매개변수가 없으면 빈 맵이다. */
public record Match(String handler, Map<String, String> params) {
}
