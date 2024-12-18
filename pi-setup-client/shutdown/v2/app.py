import logging
import subprocess

import isodate
from werkzeug.serving import run_simple
from flask import Flask, request

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        # logging.FileHandler(log_file),  # Logs in Datei schreiben
        logging.StreamHandler()
    ]
)
app = Flask(__name__)

@app.route('/')
def index():
    logging.info("happy logging")
    beep(800)
    return "happy"

@app.post('/ctl-hardware/shutdown/me')
def shutdown_me():
    delay_param = request.args.get('delay')
    if delay_param is None:
        delay_param = "PT0S"
    delay = isodate.parse_duration(delay_param)

    # subprocess.call(['shutdown', '-h', delay.seconds])

    return "shutting down in %s seconds" % delay.total_seconds()

def beep(frequency: int):
    subprocess.call([
        "timeout", "0.2s",
        "speaker-test",
            "--test", "sine",
            "--frequency", frequency,
            "--nloops", "1",
            "--scale", "140",
            "--channels", "1"
    ])

if __name__ == '__main__':
    run_simple('0.0.0.0', 8080, app)