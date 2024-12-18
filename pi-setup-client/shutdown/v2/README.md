# shutdown host

## releasing

dev machine:
1. `release.sh`

server:

```
ssh clemens@192.168.0.110
mkdir shutdown
tar -xzvf /tmp/shutdown.tar.gz -C ~/shutdown
cd shutdown/
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py
```