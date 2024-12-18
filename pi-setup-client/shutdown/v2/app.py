import logging
import subprocess
import time

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

    start_timer(delay.total_seconds())
    shutdown()
    return "shutting down initialized. bye."

def start_timer(duration_s):
    remaining_s = duration_s
    start_time = time.time()
    while time.time() - start_time < duration_s:
        logging.info("remaining %s s", remaining_s)
        if remaining_s > 10 and remaining_s % 2 == 0:
            beep(800)
        if remaining_s <= 10:
            beep(1250)
        time.sleep(1)
        remaining_s -= 1


def beep(frequency: int):
    logging.info("beep frequency=%d" % frequency)
    subprocess.call([
        "/usr/bin/timeout", "0.2s",
        "/usr/bin/speaker-test",
            "--test", "sine",
            "--frequency", str(frequency),
            "--nloops", "1",
            "--scale", "140",
            "--channels", "1"
    ])

def shutdown():
    logging.warning("shutting down NOW")
    subprocess.call(["/usr/bin/sudo", '/usr/sbin/shutdown', '-P', "now"])

if __name__ == '__main__':
    run_simple('0.0.0.0', 8080, app)