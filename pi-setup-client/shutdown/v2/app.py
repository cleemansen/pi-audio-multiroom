import subprocess

import isodate
from werkzeug.serving import run_simple
from flask import Flask, request

app = Flask(__name__)

@app.route('/')
def index():
    return "happy"

@app.post('/ctl-hardware/shutdown/me')
def shutdown_me():
    delay_param = request.args.get('delay')
    if delay_param is None:
        delay_param = "PT0S"
    delay = isodate.parse_duration(delay_param)

    # subprocess.call(['shutdown', '-h', delay.seconds])
    subprocess.call(["timeout", "0.2s", "speaker-test", "--test", "sine", "--frequency", "800", "--nloops", "1", "--scale", "140", "--channels", "1"])
    return "shutting down in %s seconds" % delay.total_seconds()

if __name__ == '__main__':
    run_simple('0.0.0.0', 8080, app)