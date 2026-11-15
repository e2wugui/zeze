import runpy
import sys
from pathlib import Path
sys.argv[1:1] = ['--section', 'recovery']
runpy.run_path(str(Path(__file__).resolve().parents[1] / 'checkpoint/check.py'), run_name='__main__')
