# Nanoleaf

# Debian Build
1. `docker run -v $(pwd):/root --rm dylanowen/sbt-packager-debian sbt debian:packageBin`
2. `scp target/nanoleaf_0.1_all.deb pi@raspberrypi.local:~/`
3. `ssh pi@raspberrypi.local`
4. `sudo dpkg -i nanoleaf_0.1_all.deb && sudo apt-get install -f`

### Uninstall
`sudo apt-get remove nanoleaf`

### Update
1. `docker run -v $(pwd):/root --rm dylanowen/sbt-packager-debian sbt debian:packageBin && scp target/nanoleaf_0.1_all.deb pi@raspberrypi.local:~/`
2. `sudo apt-get remove nanoleaf && sudo dpkg -i nanoleaf_0.1_all.deb && sudo apt-get install -f`

### Log Directory
`/var/log/nanoleaf`

### View stdout
`sudo journalctl -u nanoleaf`

### Start / Stop / Restart / Status
`sudo systemctl start nanoleaf`
`sudo systemctl stop nanoleaf`
`sudo systemctl restart nanoleaf`
`sudo systemctl status nanoleaf`

### Service File
`/etc/systemd/system/multi-user.target.wants/nanoleaf.service`

# Simple Build
`sbt assembly`

## Push to Raspberry Pi
1. `scp target/scala-2.12/nanoleaf-assembly-0.1.jar pi@raspberrypi.local:~/nanoleaf`
2. `java -jar nanoleaf-assembly-0.1.jar &` to start in the background
3. `pkill -f 'nanoleaf\-assembly'` to kill the process

## Sites:
http://www.firewall.cx/networking-topics/protocols/domain-name-system-dns/160-protocols-dns-query.html
https://github.com/posicks/mdnsjava/tree/6f304c4f4a36b92d3a73be31820bbd9f02615a8f/mdnsjava/src/main/java/net/posick/mDNS
https://doc.akka.io/docs/akka/2.5/io-udp.html