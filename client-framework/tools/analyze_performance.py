#!/usr/bin/env python3
"""Summarize bounded local timing reports. Correlation is not a root-cause verdict."""
from pathlib import Path
import argparse,json,math

def number(value):
 return value if isinstance(value,(int,float)) and not isinstance(value,bool) and math.isfinite(value) else None

def summarize(report,timeline=()):
 timings=report.get('timings',{});channels={m.get('name'):m for m in timings.get('channels',[]) if isinstance(m,dict)}
 interval=channels.get('frame_start_interval',{});frame=channels.get('client_frame_loop',{});mean=number(interval.get('mean_ms'))
 out=['# SAO 客戶端效能觀測報告','','本報告顯示量測證據，不會自動修改遊戲或宣判某個模組造成卡頓。','',
      f"觀測時間：{report.get('elapsed_seconds','未知')} 秒。",
      '估計平均顯示節奏：'+(f'{1000/mean:.1f} FPS（由幀起點間隔換算）' if mean and mean>0 else '資料不足'),'',
      '| 範圍 | 完成次數 | P50 / P95 / P99（ms） | 最長（ms） | ≥50ms 次數 |',
      '|---|---:|---|---:|---:|']
 labels={'client_frame_loop':'整個幀循環','client_tick':'客戶端 tick','game_render':'遊戲渲染呼叫','window_update_wait':'視窗更新與等待','ttf_rasterize':'TTF 字形生成','msdf_measure':'MSDF 字寬量測','msdf_draw':'MSDF 文字繪製','frame_start_interval':'幀起點間隔','glyph_disk_read':'字形磁碟讀取','glyph_disk_write':'字形磁碟寫入'}
 def display(v):return f'{v:.3f}' if number(v) is not None else '未量到'
 for key,label in labels.items():
  row=channels.get(key,{});count=number(row.get('count')) or 0
  out.append(f"| {label} | {int(count)} | {' / '.join(display(row.get(x)) for x in ('p50_ms','p95_ms','p99_ms'))} | {display(row.get('max_ms'))} | {row.get('over_50ms_count',0)} |")
 out+=['','百分位數取最近最多 4,096 筆；最長與次數屬本次完整觀測。範圍有包含關係，耗時不能直接相加。','','## 證據與限制']
 p99=number(interval.get('p99_ms'));p50=number(interval.get('p50_ms'))
 if p99 is not None and p50 is not None and p99>=33.333 and p99>p50*2:
  out.append('幀起點間隔存在明顯長尾：平均 FPS 可能尚可，但慢幀仍可造成不順。需對照相同場景與尖峰時段。')
 elif (number(interval.get('count')) or 0)==0:out.append('尚無完整幀間隔資料，不能由這份報告判斷遊戲是否順暢。')
 else:out.append('目前幀間隔資料不足以單獨定位卡頓原因；固定低幀率、垂直同步與失焦限幀仍需分開比較。')
 previous=None;overlap=0;stall_windows=0
 for sample in timeline:
  rows={m.get('name'):m for m in sample.get('timings',{}).get('channels',[]) if isinstance(m,dict)}
  total=number(rows.get('client_frame_loop',{}).get('over_50ms_count'))
  if total is None:continue
  if previous is not None and total>previous:
   stall_windows+=1
   if (number(sample.get('gc_collection_time_delta_ms')) or 0)>0:overlap+=1
  previous=total
 if stall_windows:out.append(f'發現 {stall_windows} 個取樣區間有新增慢幀，其中 {overlap} 個也有 GC 收集時間增加。這是時間區間重疊，不代表 GC 已證實為根因。')
 out+=['GPU 執行時間／使用率、遠端伺服器 TPS、網路 RTT：本版未量測，不能推估或填成 0。',
       '視窗更新耗時包含顯示同步與等待，不等於 GPU 執行時間。GC MXBean 時間不等於每段時間都停止渲染。',
       f"採樣因競爭丟棄：{timings.get('dropped_samples',0)}；異常／重入未完成計時：{timings.get('abandoned_scopes',0)}。",
       '深入分析需用同次的本地 JFR；保留關閉分析模式的同場景對照，才能估算分析工具本身的成本。',
       '不要公開未檢查的原始 JFR；其中可能包含本機檔案路徑。','','## 下一步優化準則',
       '先以相同場景定位：文字首次出現、開啟介面、區塊移動、實體密集、記憶體回收或等待。每次只調整一項，對比 P95/P99 與慢幀次數；沒有改善就撤回。','']
 return '\n'.join(out)

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('session',type=Path);args=p.parse_args()
 report_path=args.session/'summary.json';timeline_path=args.session/'timeline.jsonl'
 if report_path.stat().st_size>1024*1024:raise SystemExit('Summary exceeds budget')
 report=json.loads(report_path.read_text('utf8'));timeline=[]
 if timeline_path.exists():
  if timeline_path.stat().st_size>16*1024*1024:raise SystemExit('Timeline exceeds budget')
  timeline=[json.loads(line) for line in timeline_path.read_text('utf8').splitlines() if line.strip()]
  if len(timeline)>2000:raise SystemExit('Timeline sample count exceeds budget')
 out=args.session/'analysis.md';out.write_text(summarize(report,timeline),encoding='utf8')
 print('PERFORMANCE_REPORT_READY',out)
if __name__=='__main__':main()
