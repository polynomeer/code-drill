import { ChevronFirst, ChevronLast, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, Pause, Play } from 'lucide-react'
import { IconButton } from '../../design'
import { SPEEDS, nextMarker } from './timeline'
import type { Marker, Speed } from './timeline'
import type { TimelineMode } from './useReplayPlayer'
import styles from './ReplayPage.module.css'
import type { SeekMethod } from '../../shared/analytics'

const TIER_LABEL = { normal: '일반', important: '중요', error: '분기' } as const

/**
 * Timeline + Playback (UI 디자인 문서 §5.1·§5.3, 디자인 설계서 §8.2).
 *
 * 마커는 세 크기다 — 일반(살펴봄)·중요(상태를 바꿈)·오류(분기). 오류는 색만이 아니라 크기와 "분기"
 * 글자로도 알린다. 스크러버는 평범한 range 입력이다: 키보드·스크린리더가 그대로 다룰 수 있고,
 * `aria-valuetext` 가 걸음 번호 대신 그 걸음의 문장을 읽는다.
 */
export function ReplayTimeline({
  step,
  total,
  markers,
  important,
  playing,
  speed,
  mode,
  valueText,
  loading,
  onSeek,
  onToggle,
  onSpeed,
  onMode,
}: {
  step: number
  total: number
  markers: Marker[]
  /** Shift+←/→ 와 그 버튼이 오가는 마커. 표시 모드와 무관하게 중요·분기만이다. */
  important: Marker[]
  playing: boolean
  speed: Speed
  mode: TimelineMode
  valueText: string
  /** 지금 걸음의 구간을 받는 중 */
  loading: boolean
  onSeek: (step: number, method: SeekMethod) => void
  onToggle: () => void
  onSpeed: (speed: Speed) => void
  onMode: (mode: TimelineMode) => void
}) {
  const percent = (seq: number) => (total === 0 ? 0 : (seq / total) * 100)
  const error = markers.find((marker) => marker.tier === 'error')

  return (
    <section className={styles.timeline} aria-label="타임라인">
      <div className={styles.track}>
        <div className={styles.markers} aria-hidden="true">
          {markers.map((marker) => (
            <span
              key={`${marker.tier}-${marker.seq}`}
              className={`${styles.marker} ${styles[`marker_${marker.tier}`]}`}
              style={{ left: `${percent(marker.seq)}%` }}
              title={`${TIER_LABEL[marker.tier]} · 단계 ${marker.seq}`}
            />
          ))}
          {error && (
            <span className={styles.errorLabel} style={{ left: `${percent(error.seq)}%` }}>
              분기
            </span>
          )}
        </div>
        <input
          type="range"
          className={styles.scrubber}
          min={0}
          max={total}
          value={step}
          onChange={(event) => onSeek(Number(event.target.value), 'scrub')}
          aria-label="재생 위치"
          aria-valuetext={valueText}
        />
      </div>

      <div className={styles.playback}>
        <div className={styles.buttons}>
          <IconButton label="처음 (Home)" icon={<ChevronFirst size={18} />} onClick={() => onSeek(0, 'button')} disabled={step === 0} />
          <IconButton
            label="이전 중요 이벤트 (Shift+←)"
            icon={<ChevronsLeft size={18} />}
            onClick={() => onSeek(nextMarker(important, step, -1), 'marker')}
            disabled={nextMarker(important, step, -1) === step}
          />
          <IconButton label="이전 (←)" icon={<ChevronLeft size={18} />} onClick={() => onSeek(step - 1, 'button')} disabled={step === 0} />
          <IconButton
            label={playing ? '일시 정지 (Space)' : '재생 (Space)'}
            icon={playing ? <Pause size={18} /> : <Play size={18} />}
            onClick={onToggle}
            className={styles.play}
            disabled={total === 0}
          />
          <IconButton label="다음 (→)" icon={<ChevronRight size={18} />} onClick={() => onSeek(step + 1, 'button')} disabled={step >= total} />
          <IconButton
            label="다음 중요 이벤트 (Shift+→)"
            icon={<ChevronsRight size={18} />}
            onClick={() => onSeek(nextMarker(important, step, 1), 'marker')}
            disabled={nextMarker(important, step, 1) === step}
          />
          <IconButton label="끝 (End)" icon={<ChevronLast size={18} />} onClick={() => onSeek(total, 'button')} disabled={step >= total} />
        </div>

        <span className={styles.counter}>
          <span className="visually-hidden">단계 </span>
          {step} / {total}
          {loading && <span className={styles.loadingNote}> · 구간을 불러오는 중</span>}
        </span>

        <div className={styles.options}>
          <fieldset className={styles.segmented}>
            <legend className="visually-hidden">재생 속도</legend>
            {SPEEDS.map((value) => (
              <label key={value} className={speed === value ? styles.segmentOn : styles.segment}>
                <input
                  type="radio"
                  name="replay-speed"
                  className="visually-hidden"
                  checked={speed === value}
                  onChange={() => onSpeed(value)}
                />
                {value}x
              </label>
            ))}
          </fieldset>
          <fieldset className={styles.segmented}>
            <legend className="visually-hidden">타임라인 표시</legend>
            {(['summary', 'all'] as const).map((value) => (
              <label key={value} className={mode === value ? styles.segmentOn : styles.segment}>
                <input
                  type="radio"
                  name="replay-mode"
                  className="visually-hidden"
                  checked={mode === value}
                  onChange={() => onMode(value)}
                />
                {value === 'summary' ? '중요만' : '전체'}
              </label>
            ))}
          </fieldset>
        </div>
      </div>
    </section>
  )
}
