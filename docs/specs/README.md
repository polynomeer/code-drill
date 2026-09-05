# 기준 문서 (원본)

> **역할**: 제품·기술·디자인 결정의 원본은 무엇이고 어느 파일에 있는가
> **단일 출처**: 이 디렉터리의 `.docx`가 모든 제품 결정의 원본이다
> **갱신 트리거**: `.docx`를 추가·개정할 때 (개정 시 [../project-context.md](../project-context.md)의 어긋난 항목도 함께 고친다)

`.docx`는 바이너리라 diff가 남지 않는다. 개정할 때는 커밋 본문에 **무엇이 바뀌었는지**
적어 이력을 남긴다.

| 파일 | 답하는 질문 |
|---|---|
| `code_drill_product_plan.docx` | 제품 전략, 포지셔닝, 로드맵, 전체 기능 인벤토리 |
| `code_drill_prd.docx` | 무엇을 왜 만드는가, MVP 범위, 출시 판단 기준 |
| `code_drill_technical_design.docx` | 아키텍처, 데이터 모델, API, 채점 프로토콜, 샌드박스, 트레이스 스키마 |
| `code_drill_design_spec.docx` | 화면 구조, 상호작용 규칙, 상태와 예외, 접근성, 분석 이벤트 |
| `code_drill_ui_design_document.docx` | 디자인 시스템, 컴포넌트 동작, 반응형 규칙, 검수 기준 |

## 읽는 법

코드를 쓸 때 매번 필요한 결정(스택·모듈 경계·용어·상태 머신·신뢰 경계)은
[../project-context.md](../project-context.md)에 뽑아 두었다. 거기에 없는 상세가
필요할 때만 해당 `.docx`를 연다.

```bash
python3 -c "
import zipfile,re,sys
x=zipfile.ZipFile(sys.argv[1]).read('word/document.xml').decode()
print(re.sub('<[^>]+>','',re.sub('</w:p>','\n',x)))
" docs/specs/code_drill_technical_design.docx
```
