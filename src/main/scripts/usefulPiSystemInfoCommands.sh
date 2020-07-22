vcgencmd version

for var in arm gpu ; do \
  echo -e "$var:\t$(vcgencmd get_mem $var)"; \
done

for var in arm core h264 isp v3d uart pwm emmc pixel vec hdmi dpi ; do \
  echo -e "$var:\t$(vcgencmd measure_clock $var)"; \
done

for var in core sdram_c sdram_i sdram_p ; do \
  echo -e "$var:\t$(vcgencmd measure_volts $var)" ; \
done

for var in H264 MPG2 WVC1 MPG4 MJPG WMV9 ; do \
  echo -e "$var:\t$(vcgencmd codec_enabled $var)" ; \
done

for var in config int str ; do \
  echo -e "$var:\t$(vcgencmd get_config $var)" ; \
done

vcgencmd measure_temp
