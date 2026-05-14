#!/bin/sh

#----------------------------------------------------------------------------------------
  # squeezelite -S /usr/local/bin/squeezelite-power.sh
  # 
  # squeezelite sets $1 to:
  #     0: off
  #     1: on
  #     2: initialising

  # kudos: https://www.mail-archive.com/unix@lists.slimdevices.com/msg57575.html
  
#----------------------------------------------------------------------------------------
  # Version: 0.1 2022-12-27

echo $1 >> /tmp/squeezelite-lifecycle
case $1 in
  0)
    rm /tmp/squeezelite-*-signal
    touch /tmp/squeezelite-off-signal
    # start in background and disowned: https://unix.stackexchange.com/a/608872
    nohup bash /usr/local/bin/squeezelite-system-shutdown-now.sh >/dev/null 2>&1 &
    ;;
  1)
    touch /tmp/squeezelite-on-signal
    ;;
  2)
    touch /tmp/squeezelite-init-signal
    ;;
esac