#! /bin/bash
### BEGIN INIT INFO
# Provides:          Squeezelite
# Required-Start:
# Required-Stop:
# Should-Start:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Squeezelite
# Description:       Light weight streaming audio player for Logitech's Squeezebox audio server
#                    See: http://code.google.com/p/squeezelite/
#                    and: http://forums.slimdevices.com/showthread.php?97046-Announce-Squeezelite-a-small-headless-squeezeplay-emulator-for-linux-%28alsa-only%29
### END INIT INFO

# Script version 1.14

# See for full install instructions:  http://www.gerrelt.nl/RaspberryPi/wordpress/tutorial-installing-squeezelite-player-on-raspbian/
# Uninstall Instructions :  update-rc.d squeezelitehf remove

# PATH should only include /usr/* if it runs after the mountnfs.sh script
PATH=/sbin:/usr/sbin:/bin:/usr/bin
DESC="Squeezebox client"
NAME=squeezelite-armv6hf
SL_DOWNLOAD_URL="http://www.gerrelt.nl/RaspberryPi/squeezelite_ralph/squeezelite-armv6hf.tar.gz"
#SL_DOWNLOAD_URL="http://ralph_irving.users.sourceforge.net/pico/squeezelite-armv6hf-noffmpeg"
# alternative (older version): http://squeezelite-downloads.googlecode.com/git/squeezelite-armv6hf

LINUX_DISTRO=$(uname -r)
if [[ $LINUX_DISTRO == *"piCore"* ]]
then
  DAEMON=/mnt/mmcblk0p2/tce/${NAME}
else
  DAEMON=/usr/bin/${NAME}
fi

PIDFILE=/var/run/${NAME}.pid
SCRIPTNAME=/etc/init.d/squeezelite
# get mac address from wifi adapter or on board network card
SL_MAC_ADDRESS=$(cat /sys/class/net/wlan0/address)
[ -n "$SL_MAC_ADDRESS" ] || SL_MAC_ADDRESS=$(cat /sys/class/net/eth0/address)

# get hostname which can be used as hostname
# watch out, on raspbian, you can only use letters, numbers and hyphens (minus sign, "-"). And nothing else!
SL_NAME=$(hostname -s)
[ -n "$SL_NAME" ] || SL_NAME=SqueezelitePlayer
# Get squeezelite version, for logging and update procedure
SL_VERSION=$(sudo $DAEMON -t | grep "Squeezelite v" | tr -s ' ' | cut -d ',' -f1 | cut -d ' ' -f2)
# Squeezebox server port for sending play and power off commands
SB_SERVER_CLI_PORT="9090"

# Exit if the package is not installed
if [ ! -x "$DAEMON" ]
then
  echo "Error: $DAEMON not found."
  exit 2
fi

# Get user settings for squeezelite (it should contain settings like SL_SOUNDCARD, SB_SERVER_IP, SL_ALSA_PARAMS etc.)
# For the default script, see: http://www.gerrelt.nl/RaspberryPi/squeezelite_settings.sh
SL_SETTINGS=/usr/local/bin/squeezelite_settings.sh
if [ ! -x "$SL_SETTINGS" ]
then
  echo "Error: script $SL_SETTINGS not found."
  exit 2
else
  . $SL_SETTINGS
fi

if [ ! -z "$SL_MAC_ADDRESS" ]; then

  if [[ "${SL_MAC_ADDRESS^^}" == "RANDOM" ]]; then
    # generate random mac address
    hexchars="0123456789ABCDEF"
    RANDOMMAC=$( for i in {1..12} ; do echo -n ${hexchars:$(( $RANDOM % 16 )):1} ; done | sed -e 's/\(..\)/:\1/g' )
    SL_MAC_ADDRESS=${RANDOMMAC:1}
  fi
  # check if valid mac address is obtained
  if [[ ! "$SL_MAC_ADDRESS" =~ ^([a-fA-F0-9]{2}:){5}[a-fA-F0-9]{2}$ ]]; then
	echo "Warning: no valid mac address found in SL_MAC_ADDRESS, create mac address from player name."
	HEXVAL=$(printf "%02s" 12)$(xxd -pu <<< "$SL_NAME")
    HEXVAL=000000000000${HEXVAL:0:12}
    SL_MAC_ADDRESS=$(echo ${HEXVAL: -12} | sed 's/.\{2\}/&:/g' | rev | cut -c 2- | rev)
  fi
  if [[ ! "$SL_MAC_ADDRESS" =~ ^([a-fA-F0-9]{2}:){5}[a-fA-F0-9]{2}$ ]]; then
    echo "Warning: no valid mac address found in SL_MAC_ADDRESS, let squeezelite determine mac address."
	unset SL_MAC_ADDRESS
  fi
fi

# Read configuration variable file if it is present
[ -r /etc/default/$NAME ] && . /etc/default/$NAME

# Load the VERBOSE setting and other rcS variables
[ -r /lib/init/vars.sh ] && . /lib/init/vars.sh

# Define LSB log_* functions.
# Depend on lsb-base (>= 3.2-14) to ensure that this file is present
# and status_of_proc is working.
[ -r /lib/lsb/init-functions ] && . /lib/lsb/init-functions

#
# Function that starts the daemon/service
#
do_start()
{

    # Return
    #   0 if daemon has been started
    #   1 if daemon was already running
    #   2 if daemon could not be started
    
    # check if squeezelite is allready running
    start-stop-daemon -K --quiet --pidfile $PIDFILE --test
    if [ "$?" == 0 ]; then
      echo "Squeezelite already running (checked pidfile: $PIDFILE)."
      return 1
    fi

    DAEMON_START_ARGS=""
    
    # set the working directory for squeezelite
    if [ ! -z "$SL_WORKING_DIR" ]; then
       DAEMON_START_ARGS="${DAEMON_START_ARGS} --chdir ${SL_WORKING_DIR}"    
    fi

    # set the user which will be used to start squeezelite
    if [ ! -z "$SL_USER" ]; then
       DAEMON_START_ARGS="${DAEMON_START_ARGS} --chuid ${SL_USER}"    
    fi

    DAEMON_ARGS=""    
    
    # add souncard setting if set
    if [ ! -z "$SL_SOUNDCARD" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -o ${SL_SOUNDCARD}"    
    fi

    # add squeezelite name if set
    if [ ! -z "$SL_NAME" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -n ${SL_NAME}"
    fi
    
    # add mac address if set
    if [ ! -z "$SL_MAC_ADDRESS" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -m ${SL_MAC_ADDRESS}"    
    fi

    # add squeezebox server ip address if set
    if [ ! -z "$SB_SERVER_IP" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -s ${SB_SERVER_IP}"    
    fi
    
    # set ALSA parameters if set
    if [ ! -z "$SL_ALSA_PARAMS" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -a ${SL_ALSA_PARAMS}"    
    fi
    
    # add logging if set
    if [ ! -z "$SL_LOGFILE" ]; then
       if [ -f ${SL_LOGFILE} ]; then
          rm ${SL_LOGFILE}
       fi
       DAEMON_ARGS="${DAEMON_ARGS} -f ${SL_LOGFILE}"    
    fi

    # add log level setting if set
    if [ ! -z "$SL_LOGLEVEL" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} -d ${SL_LOGLEVEL}"    
    fi

    # add additional options if set
    if [ ! -z "$SL_ADDITIONAL_OPTIONS" ]; then
       DAEMON_ARGS="${DAEMON_ARGS} ${SL_ADDITIONAL_OPTIONS}"    
    fi
    
	# Let squeezelite create the PIDFILE
	DAEMON_ARGS="${DAEMON_ARGS} -P $PIDFILE -z" 
	
    echo "Starting: $DAEMON $DAEMON_ARGS"
    echo "with pidfile: $PIDFILE"
    start-stop-daemon --start --quiet --pidfile $PIDFILE --background $DAEMON_START_ARGS --exec $DAEMON -- $DAEMON_ARGS || return 2 
	# In this command start-stop-daemon creates the PIDFILE (see make-pidfile option), but somehow this doesn't always work on boot:
    #start-stop-daemon --start --quiet --make-pidfile --pidfile $PIDFILE --background $DAEMON_START_ARGS --exec $DAEMON -- $DAEMON_ARGS || return 2 
    # If start-stop-daemon doesn't work, use this instead:
    #$DAEMON $DAEMON_ARGS -P $PIDFILE -z

    # next commands can only be done if Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Wait until player is connected to Squeezebox server before sending play command"
      for i in $(seq 1 15)
      do
        PLAYERCONNECTED=$(printf "$SL_NAME connected ?\nexit\n" | nc -w 1 $SB_SERVER_IP $SB_SERVER_CLI_PORT  | tr -s ' '| cut -d ' ' -f3)
        if [ "$PLAYERCONNECTED" == "1" ]
        then
          echo "Player connected to Squeezebox server after $i seconds"
          break
        fi
        echo "Not connected after $i seconds..."
        sleep 1
      done
      
      if [ "$PLAYERCONNECTED" == "1" ]
      then
        # connected
      
        # First send power-on command to squeezebox server
        echo "Sending power on command for player ${SL_NAME} (${SL_MAC_ADDRESS}) to Squeezebox server (${SB_SERVER_IP} ${SB_SERVER_CLI_PORT})"
        printf "$SL_MAC_ADDRESS power 1\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
      
        # check if auto play is set
        if [ ! -z "$SL_AUTO_PLAY" ] && [ "${SL_AUTO_PLAY^^}" == "YES" ]; then
          if [ ! -z "$SL_AUTO_PLAY_FAV" ]; then
            do_play_fav $SL_AUTO_PLAY_FAV $SL_AUTO_PLAY_VOLUME
          else
            do_play $SL_AUTO_PLAY_VOLUME
          fi
        fi
      else
        echo "Could not send play command to player $SL_NAME on Squeezebox server $SB_SERVER_IP" 
      fi
      
    fi
}

#
# Function that "switches off" the player. It's not really switching off the player, because it's a software player.
# But the LMS server will see the player as being powered off.
#
do_poweroff()
{
    # First send power-off command to squeezebox server, can only be done if Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending power off command for player ${SL_NAME} (${SL_MAC_ADDRESS}) to Squeezebox server (${SB_SERVER_IP} ${SB_SERVER_CLI_PORT})"
      printf "$SL_MAC_ADDRESS power 0\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the poweroff function."
    fi
}

#
# Function that "switches on" the player. It's not really switching on the player, because it's a software player.
# But the LMS server will see the player as being powered on.
#
do_poweron()
{
    # First send power-on command to squeezebox server, can only be done if Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending power on command for player ${SL_NAME} (${SL_MAC_ADDRESS}) to Squeezebox server (${SB_SERVER_IP} ${SB_SERVER_CLI_PORT})"
      printf "$SL_MAC_ADDRESS power 1\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the poweron function."
    fi
}

#
# Function that stops the daemon/service
#
do_stop()
{
    # First send power-off command to squeezebox server, can only be done if Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending power off command for player ${SL_NAME} (${SL_MAC_ADDRESS}) to Squeezebox server (${SB_SERVER_IP} ${SB_SERVER_CLI_PORT})"
      printf "$SL_MAC_ADDRESS power 0\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
    fi
    
    # Return
    #   0 if daemon has been stopped
    #   1 if daemon was already stopped
    #   2 if daemon could not be stopped
    #   other if a failure occurred
    start-stop-daemon --stop --quiet --retry=TERM/30/KILL/5 --pidfile $PIDFILE --exec $DAEMON
    RETVAL="$?"
    [ "$RETVAL" = 2 ] && return 2
    # Wait for children to finish too if this is a daemon that forks
    # and if the daemon is only ever run from this initscript.
    # If the above conditions are not satisfied then add some other code
    # that waits for the process to drop all resources that could be
    # needed by services started subsequently.  A last resort is to
    # sleep for some time.
    start-stop-daemon --stop --quiet --oknodo --retry=0/30/KILL/5 --exec $DAEMON
    [ "$?" = 2 ] && return 2
    # Many daemons don't delete their pidfiles when they exit.
    rm -f $PIDFILE
    return "$RETVAL"
}

#
# Function that updates squeezelite
#
do_update()
{
    mkdir -p /tmp/sl_download
    wget -P /tmp/sl_download ${SL_DOWNLOAD_URL}
	tar -xvzf /tmp/sl_download/squeezelite-armv6hf.tar.gz -C /tmp/sl_download
    mv /tmp/sl_download/squeezelite /tmp/sl_download/${NAME}
    sudo cp $DAEMON /tmp/${NAME}.old
    #SL_DOWNLOAD_NAME=$(echo ${SL_DOWNLOAD_URL} | rev | cut -d '/' -f1 | rev)
    sudo mv /tmp/sl_download/${NAME} $DAEMON
	#cleanup download directory
	rm -r /tmp/sl_download
    sudo chmod u+x $DAEMON
    # get the new version
    SL_VERSION=$(sudo $DAEMON -t | grep "Squeezelite v" | tr -s ' ' | cut -d ',' -f1 | cut -d ' ' -f2)
}

#
# Function for telling the player to start playing at a certain volume (optional)
#
# cronjob:
#0 7 * * 1-5 sudo /etc/init.d/squeezelite play 40
#
do_play()
{
    VOLUME=$1
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending play command to Squeezebox server"
      printf "$SL_NAME play\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
      if  [ ! -z "$1" ]; then
         # volume has to be set
         do_set_volume "$VOLUME"
      fi
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play function."
    fi
}

#
# Play next or previous song
#
do_play_nextprev()
{
  # This function only works if the Squeezebox server IP is set
  if  [ ! -z "$SB_SERVER_IP" ]; then
    DIRECTION=$1
    if [ -z "$DIRECTION" ]; then
       # parameter empty / not given, do a next
       DIRECTION="NEXT"
    fi

    # check if we're going forwards or backwards
    if [ "$DIRECTION" == "NEXT" ]; then
      UPDOWN="+1"
    else 
      UPDOWN="-1"
    fi
    printf "$SL_NAME  playlist index ${UPDOWN}\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT
  else
    echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
  fi
}

#
# Play next song
#
do_play_next()
{
  do_play_nextprev "NEXT"
}

#
# Play previous song
#
do_play_prev()
{
  do_play_nextprev "PREVIOUS"
}

#
# Function to play something from the favorite list at a certain volume (optional)
# Note: replace all spaces in the favorite name with %20
#
# cronjob:
#0 7 * * 1-5 sudo /etc/init.d/squeezelite play_fav "Q-music" 40
#
do_play_fav()
{
    SEARCHFOR=$1
    VOLUME=$2
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      FAV_ID=$(printf "$SL_NAME favorites items 0 1000\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT  | sed 's/%3A/:/g' | sed 's/ id:/\'$'\n/g' | grep -i "${SEARCHFOR}" | cut -d ':' -f1 | cut -d ' ' -f1 | head -n 1)
      echo $FAV_ID
      printf "$SL_NAME favorites playlist play item_id:${FAV_ID}\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT
      if  [ ! -z "$2" ]; then
         # volume has to be set
         do_set_volume "$VOLUME"
      fi
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
    fi
}

#
# Function that return the favorites list
#
list_favorites() {
  # This function only works if the Squeezebox server IP is set
  if  [ ! -z "$SB_SERVER_IP" ]; then
    # Get favorites list, and echo it
    # (decode URL escape characters twice because of url)
    FAVLIST=$(printf "$SL_NAME favorites items 0 1000 want_url:1\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/ id:([0-9]+) /\n\1 /g'   | sed -r 's/ (count:[0-9]+)$/\n\1/g')
    echo "$FAVLIST"
  else
    echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
  fi
}

#
# Find the currently playing favorite by stream url, mp3 filename, album, artist or genre
#
get_current_fav_id() {
  # for debugging output to stderror, otherwise it will be the result of this function!
  #echo "$FAVLIST" >&2

  # This function only works if the Squeezebox server IP is set
  if  [ ! -z "$SB_SERVER_IP" ]; then

    FAVLIST="$1" # de double quotes preserve the newlines, otherwise they will be gone..
    if [ -z "$FAVLIST" ]; then
      echo "function get_current_fav_id needs parameter FAVLIST, which contains the favorites list from function list_favorites" >&2
      return 1
    fi

    # check if it's a radio stream currently playing
    RADIO_STREAM=$(printf "$SL_NAME playlist remote ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/.* ([0-9]*)$/\1/g')


    # If remote is 1, then it's a radio station, search on url.
    if [ "$RADIO_STREAM" == "1" ]
    then
      # It's a radio stream, match on url
      #
      # example output: b8:27:eb:97:75:86 path ? http://icecast.omroep.nl/3fm-sb-aac
      SEARCH_STRING="url:$(printf "$SL_NAME path ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/^.* path (.*)$/\1/g' | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e)"
      # Get current favorite using: $SEARCH_STRING
      # : 0 name:3FM type:audio url:http://icecast.omroep.nl/3fm-sb-aac isaudio:1 hasitems:0
      CURRENT_FAV_ID=$(echo "$FAVLIST" | grep " type:audio $SEARCH_STRING isaudio:[0-1] hasitems:[0-9]" | sed -r 's/^([0-9]+) name:.*$/\1/g')
    else
      # Remote is 0, then it's a local file.

      # Try to find mp3 filename first
      #
      # example output: b8:27:eb:97:75:86 path ? file:///share/Muziek/Muse%20-%20Drones/03%20Muse%20-%20Psycho.mp3
      SEARCH_STRING="url:$(printf "$SL_NAME path ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/^.* path (.*)$/\1/g' | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e)"
      # Get current favorite using: $SEARCH_STRING
      CURRENT_FAV_ID=$(echo "$FAVLIST" | grep " type:audio $SEARCH_STRING isaudio:[0-1] hasitems:[0-9]" | sed -r 's/^([0-9]+) name:.*$/\1/g')

      if [ -z "$CURRENT_FAV_ID" ]; then
        # Try album
        #
        # example output: b8:27:eb:97:75:86 album ? 2015 1 Romantische Rijn
        SEARCH_STRING="url:db:album.title=$(printf "$SL_NAME album ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/^.* album (.*)$/\1/g')"
        # Get current favorite using: $SEARCH_STRING
        CURRENT_FAV_ID=$(echo "$FAVLIST" | grep " type:audio $SEARCH_STRING isaudio:[0-1] hasitems:[0-9]" | sed -r 's/^([0-9]+) name:.*$/\1/g')
      fi
      if [ -z "$CURRENT_FAV_ID" ]; then
        # Try artist
        #
        # example output: b8:27:eb:97:75:86 artist ? U2
        SEARCH_STRING="url:db:contributor.name=$(printf "$SL_NAME artist ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/^.* artist (.*)$/\1/g')"
        # Get current favorite using: $SEARCH_STRING
        CURRENT_FAV_ID=$(echo "$FAVLIST" | grep " type:audio $SEARCH_STRING isaudio:[0-1] hasitems:[0-9]" | sed -r 's/^([0-9]+) name:.*$/\1/g')
      fi
      if [ -z "$CURRENT_FAV_ID" ]; then
        # Try genre
        #
        # example output: b8:27:eb:97:75:86 genre ? Alt. Rock
        SEARCH_STRING="url:db:genre.name=$(printf "$SL_NAME genre ?\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT | sed -e's/%\([0-9A-F][0-9A-F]\)/\\\\\x\1/g' | xargs echo -e | sed -r 's/^.* genre (.*)$/\1/g')"
        # Get current favorite using: $SEARCH_STRING
        CURRENT_FAV_ID=$(echo "$FAVLIST" | grep " type:audio $SEARCH_STRING isaudio:[0-1] hasitems:[0-9]" | sed -r 's/^([0-9]+) name:.*$/\1/g')
      fi
    fi
    # return the favorite id
    echo "$CURRENT_FAV_ID"

  else
    echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
  fi
}

#
# Plays the next or previous favorite
#
play_nextprev_favorite() {

  # This function only works if the Squeezebox server IP is set
  if  [ ! -z "$SB_SERVER_IP" ]; then

    DIRECTION=$1
    if [ -z "$DIRECTION" ]; then
       # parameter empty / not given, do a next
       DIRECTION="NEXT"
    fi
    # Get favorite list
    FAVLIST=$(list_favorites)
    #echo "$FAVLIST"
    # get number of favorites
    NUMBER_FAVS=$(echo "$FAVLIST" | grep "^count:[0-9]*" | sed -r 's/^count:([0-9]*)$/\1/g')
    #echo Number of favorites: $NUMBER_FAVS
    # this function uses a temporary file to keep track of the current favorite, once it's determind with the get_current_fav_id function
    # check if file with current favorite exists
    CURFAV_FILE="/run/squeezelite_curfav.txt"
    if [ -f "$CURFAV_FILE" ]; then
      # if yes, then use it to determine next favorite
      #echo "$CURFAV_FILE found."
      
      FAV_NUMBER=$(cat $CURFAV_FILE)
      
    else
      # if no, then search the favorite with current song
      #echo "$CURFAV_FILE not found."
      FAV_NUMBER=$(get_current_fav_id "$FAVLIST")
    fi
    if [ -z "$FAV_NUMBER" ]; then
      # no current ID found, so set it to the first favorite
      FAV_NUMBER=0
      #echo Current favorite not found, go to: $FAV_NUMBER
    else
      # Get the favorite number
      
      # check if we're going forwards or backwards
      if [ "$DIRECTION" == "NEXT" ]; then
        ((FAV_NUMBER++))
      else 
        ((FAV_NUMBER--))
      fi

      # If the last favorite number is reached, go to the first (0) one.
      if [ "$FAV_NUMBER" == "$NUMBER_FAVS" ]; then
        FAV_NUMBER=0
      fi
      # If the first favorite number (0) was reached, go to the last one.
      if [ "$FAV_NUMBER" == "-1" ]; then
        FAV_NUMBER=$((NUMBER_FAVS-1))
      fi

    fi
    #echo Current favorite ID: $FAV_NUMBER
    # Write the new favorite ID to the temporary file
    echo $FAV_NUMBER > $CURFAV_FILE
    # Play the new favorite ID:
    printf "$SL_NAME favorites playlist play item_id:${FAV_NUMBER}\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
  else
    echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
  fi
}

#
# Plays the next favorite
#
play_next_favorite() {
  play_nextprev_favorite "NEXT"
}

#
# Plays the previous favorite
#
play_prev_favorite() {
  play_nextprev_favorite "PREVIOUS"
}

#
# Clears the current playlist of all songs.
#
clear_playlist()
{
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending clear playlist command to Squeezebox server"
      printf "$SL_NAME playlist clear\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the clear_playlist function."
    fi
}


#
# Function for telling the player to stop playing
#
# cronjob:
#0 7 * * 1-5 sudo /etc/init.d/squeezelite stop_playing
#
do_stop_playing()
{
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      echo "Sending stop playing command to Squeezebox server"
      printf "$SL_NAME stop\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT > /dev/null
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the stop_playing function."
    fi
}

#
# Function to set the volume
#
# cronjob:
#0 7 * * 1-5 sudo /etc/init.d/squeezelite set_volume 40
#
do_set_volume()
{
    VOLUME=$1
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
      if  [ ! -z "$1" ]; then
         # volume has to be set
         printf "$SL_NAME mixer volume ${VOLUME}\nexit\n" | nc -w 10 $SB_SERVER_IP $SB_SERVER_CLI_PORT
      else
         echo "ERROR: set_volume needs a volume as a parameter, for example: /etc/init.d/squeezelite set_volume 40"
      fi
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the play_fav function."
    fi
}

#
# Function to handle librespot events
#
do_librespot_event()
{
    # This function only works if the Squeezebox server IP is set
    if  [ ! -z "$SB_SERVER_IP" ]; then
       
      # variable "PLAYER_EVENT" is set by Librespot.
    
      if [ "$PLAYER_EVENT" == "start" ]; then
        # librespot want to start playing, so power-off squeezelite to free the soundcard.
        do_poweroff
#      elif [ "$PLAYER_EVENT" == "stop" ]; then
        # librespot stopped playing, so power-on squeezelite again.
 #       do_poweron
      fi
    else
      echo "The IP address of the Squeezebox server is not set (variable: SB_SERVER_IP should be set). This is needed for the do_librespot_event function."
    fi
}


#
# Function that sends a SIGHUP to the daemon/service
#
do_reload() {
    #
    # If the daemon can reload its configuration without
    # restarting (for example, when it is sent a SIGHUP),
    # then implement that here.
    #
    start-stop-daemon --stop --signal 1 --quiet --pidfile $PIDFILE --name $NAME
    return 0
}

case "$1" in
  start)
    [ "$VERBOSE" != no ] && echo "Starting $DESC" "$NAME"
    do_start
    case "$?" in
        0|1) [ "$VERBOSE" != no ] && echo "Squeezelite running"; exit 0 ;;
        *) [ "$VERBOSE" != no ] && echo "Error trying to start squeezelite"; exit 1 ;;
    esac
    ;;
  stop)
    [ "$VERBOSE" != no ] && echo "Stopping $DESC" "$NAME"
    do_stop
    case "$?" in
        0|1) [ "$VERBOSE" != no ] && echo "Squeezelite stopped"; exit 0 ;;
        *) [ "$VERBOSE" != no ] && echo "Error trying to stop squeezelite"; exit 1 ;;
    esac
    ;;
  update)
    echo "Update Squeezelite $SL_VERSION to latest version"
    do_stop
    do_update
    do_start
    echo "Squeezelite updated to version: $SL_VERSION"
    ;;
  play)
    echo "Play with volume $2"
    do_play "$2"
    ;;
  play_nextprev)
    echo "Play $2 (NEXT|PREVIOUS) song"
    do_play_nextprev $2
    ;;
  play_next)
    echo "Play next song"
    do_play_next
    ;;
  play_prev)
    echo "Play previous song"
    do_play_prev
    ;;
  play_fav)
    echo "Play favorite $2 with volume $3"
    do_play_fav "$2" "$3"
    ;;
  list_favorites)
    echo "List all favorites"
    list_favorites
    ;;
  get_current_fav_id)
    echo "Get currently playing favorite from given favorite list"
    get_current_fav_id $2
    ;;
  play_nextprev_favorite)
    echo "Play $2 (NEXT|PREVIOUS) favorite"
    play_nextprev_favorite $2
    ;;
  play_next_favorite)
    echo "Play next favorite"
    play_next_favorite
    ;;
  play_prev_favorite)
    echo "Play previous favorite"
    play_prev_favorite
    ;;
  clear_playlist)
    echo "Clear current playlist"
    clear_playlist
    ;;
  stop_playing)
    echo "Stop playing"
    do_stop_playing
    ;;
  set_volume)
    echo "Set volume to $2"
    do_set_volume "$2"
    ;;
  status)
       status_of_proc "$DAEMON" "$NAME" && exit 0 || exit $?
       ;;
  switchoff)
    do_poweroff
    ;;
  switchon)
    do_poweron
    ;;
  librespot_event)
    do_librespot_event
    ;;
  restart|force-reload)
    #
    # If the "reload" option is implemented then remove the
    # 'force-reload' alias
    #
    echo "Restarting $DESC" "$NAME"
    echo " "
    do_stop
    case "$?" in
      0|1)
        do_start
        case "$?" in
           0) [ "$VERBOSE" != no ] && echo "Squeezelite restarted"; exit 0 ;;
           1) [ "$VERBOSE" != no ] && echo "Error trying to restart squeezelite, it couldn't be stopped"; exit 1 ;;
           *) [ "$VERBOSE" != no ] && echo "Error trying to restart squeezelite"; exit 1 ;;
        esac
        ;;
      *)
          # Failed to stop
        echo "Error trying to restart squeezelite, it couldn't be stopped"
        exit 1
        ;;
    esac
    ;;
  *)
    echo "Usage: $SCRIPTNAME {start|stop|update|play|play_fav|stop_playing|set_volume|status|restart|force-reload}" >&2
    exit 3
    ;;
esac

:
