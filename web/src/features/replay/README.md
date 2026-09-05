실행 리플레이: manifest → summary 먼저 받고 현재 위치 주변 청크를 prefetch 한다.

상태는 가장 가까운 checkpoint 에서 이벤트를 적용해 복원한다. 렌더러는
eventType + target kind 레지스트리로 고르고, 스키마 미지원 시 텍스트 이벤트 목록으로
폴백한다 (기술 설계서 §7.5).
