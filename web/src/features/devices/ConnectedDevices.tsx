import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { disconnectDevice, listDevices } from '../../api/client'
import { Button, Skeleton } from '../../design'
import { fullTime } from '../../shared/format'
import styles from './ConnectedDevices.module.css'

/**
 * 연결된 기기 (디자인 시안 23) — CLI 로 로그인한 기기. 끊으면 그 기기는 다시 `codedrill login` 해야 한다.
 * 90일 쓰지 않은 기기는 저절로 끊긴다(서버의 deviceRefreshTtl).
 */
export function ConnectedDevices() {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['me', 'devices'], queryFn: listDevices })
  const disconnect = useMutation({
    mutationFn: disconnectDevice,
    onSuccess: () => void client.invalidateQueries({ queryKey: ['me', 'devices'] }),
  })

  return (
    <section className={styles.section} aria-labelledby="devices-heading">
      <h3 id="devices-heading" className={styles.heading}>
        연결된 기기
      </h3>
      <p className={styles.muted}>
        CLI(<code>codedrill login</code>)로 연결한 기기입니다. 프로젝트형 받기·제출·초안 저장만 할 수 있고, 90일 쓰지 않으면 저절로 끊깁니다.
      </p>
      {query.isPending ? (
        <Skeleton height={40} />
      ) : query.isError ? (
        <p className={styles.muted}>기기 목록을 불러오지 못했습니다.</p>
      ) : query.data.length === 0 ? (
        <p className={styles.muted}>연결된 기기가 없습니다.</p>
      ) : (
        <ul className={styles.list}>
          {query.data.map((device) => (
            <li key={device.id} className={styles.row}>
              <div>
                <strong>{device.deviceName}</strong>
                <span className={styles.meta}>
                  {device.client}
                  {device.connectedAt && ` · ${fullTime(device.connectedAt)} 연결`}
                  {device.lastUsedAt && ` · 마지막 사용 ${fullTime(device.lastUsedAt)}`}
                </span>
              </div>
              <Button
                variant="danger"
                size="dense"
                onClick={() => disconnect.mutate(device.id)}
                loading={disconnect.isPending && disconnect.variables === device.id}
              >
                연결 끊기
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
