from pathlib import Path
import importlib.util,unittest
P=Path(__file__).resolve().parents[1]/'tools/analyze_performance.py'
spec=importlib.util.spec_from_file_location('performance_report',P);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
class PerformanceReport(unittest.TestCase):
 def test_empty_report_does_not_invent_fps_gpu_or_server_tps(self):
  text=mod.summarize({'timings':{'channels':[]}})
  self.assertIn('資料不足',text);self.assertIn('未量測',text);self.assertNotIn('0.0 FPS',text)
 def test_high_mean_fps_does_not_hide_tail_stalls(self):
  r={'elapsed_seconds':120,'timings':{'channels':[{'name':'frame_start_interval','count':5000,'mean_ms':10,'p50_ms':8,'p95_ms':16,'p99_ms':100,'max_ms':200,'over_50ms_count':50}]}}
  text=mod.summarize(r);self.assertIn('100.0 FPS',text);self.assertIn('明顯長尾',text)
 def test_gc_overlap_is_not_called_proven_cause(self):
  timeline=[{'timings':{'channels':[{'name':'client_frame_loop','over_50ms_count':n}]},'gc_collection_time_delta_ms':5} for n in (0,1,3)]
  text=mod.summarize({'timings':{'channels':[]}},timeline)
  self.assertIn('2 個也有 GC',text);self.assertIn('不代表 GC 已證實為根因',text)
 def test_non_finite_or_boolean_metrics_are_not_numbers(self):
  for x in (None,True,float('nan'),float('inf'),'10'):self.assertIsNone(mod.number(x))
if __name__=='__main__':unittest.main()
